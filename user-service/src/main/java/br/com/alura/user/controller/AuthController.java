package br.com.alura.user.controller;

import br.com.alura.user.dto.LoginRequestDTO;
import br.com.alura.user.dto.LoginResponseDTO;
import br.com.alura.user.dto.PreAuthResponseDTO;
import br.com.alura.user.dto.TwoFactorRequestDTO;
import br.com.alura.user.model.Usuario;
import br.com.alura.user.repository.UsuarioRepository;
import br.com.alura.user.security.JwtService;
import br.com.alura.user.security.TokenBlocklistService;
import br.com.alura.user.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, autenticação 2FA e logout")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final TokenBlocklistService tokenBlocklistService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          TotpService totpService,
                          TokenBlocklistService tokenBlocklistService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.totpService = totpService;
        this.tokenBlocklistService = tokenBlocklistService;
    }

    @Operation(summary = "Login com e-mail e senha")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "JWT retornado ou pré-auth token (quando 2FA ativo)"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));

        if (usuario.getSenha() == null || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }

        if (usuario.isTotpAtivo()) {
            String preAuthToken = jwtService.generatePreAuthToken(usuario.getId());
            return ResponseEntity.ok(new PreAuthResponseDTO(preAuthToken, "Informe o código 2FA do seu autenticador."));
        }

        String token = jwtService.generateToken(usuario);
        return ResponseEntity.ok(new LoginResponseDTO(token, "Bearer", 3600L));
    }

    @Operation(summary = "Validar código TOTP e obter JWT final")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "JWT retornado"),
        @ApiResponse(responseCode = "400", description = "Token fornecido não é pré-auth"),
        @ApiResponse(responseCode = "401", description = "Código OTP inválido ou token expirado")
    })
    @PostMapping("/2fa")
    public ResponseEntity<LoginResponseDTO> twoFactor(@RequestBody TwoFactorRequestDTO request) {
        if (!jwtService.validateToken(request.preAuthToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou expirado.");
        }

        Boolean pending = (Boolean) jwtService.extractClaim(request.preAuthToken(), "pending_2fa");
        if (!Boolean.TRUE.equals(pending)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token fornecido não é um pré-auth token.");
        }

        Long userId = jwtService.extractUserId(request.preAuthToken());
        boolean codigoValido = totpService.verificar(userId, request.codigoOtp());
        if (!codigoValido) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código OTP inválido ou expirado.");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        String token = jwtService.generateToken(usuario);
        return ResponseEntity.ok(new LoginResponseDTO(token, "Bearer", 3600L));
    }

    @Operation(summary = "Invalidar JWT (logout)")
    @ApiResponse(responseCode = "204", description = "Token invalidado com sucesso")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.validateToken(token)) {
                tokenBlocklistService.blacklist(token, jwtService.extractExpiry(token));
            }
        }
        return ResponseEntity.noContent().build();
    }
}
