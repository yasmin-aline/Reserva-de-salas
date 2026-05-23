package br.com.alura.user.controller;

import br.com.alura.user.dto.TotpDTO;
import br.com.alura.user.dto.TotpSetupDTO;
import br.com.alura.user.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios/{id}/2fa")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "2FA", description = "Configuração e verificação do autenticador TOTP (requer ADMIN)")
public class TotpController {

    private final TotpService totpService;

    public TotpController(TotpService totpService) {
        this.totpService = totpService;
    }

    @Operation(summary = "Gerar segredo TOTP para o usuário")
    @ApiResponse(responseCode = "200", description = "QR code e chave secreta retornados")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/setup")
    public ResponseEntity<TotpSetupDTO> setup(@PathVariable Long id) {
        TotpSetupDTO dto = totpService.gerarSegredo(id);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Ativar 2FA com código de confirmação")
    @ApiResponse(responseCode = "200", description = "2FA ativado com sucesso")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/ativar")
    public ResponseEntity<String> ativar(@PathVariable Long id, @RequestBody TotpDTO dto) {
        totpService.ativar(id, dto);
        return ResponseEntity.ok("{\"mensagem\": \"2FA ativado com sucesso.\"}");
    }

    @Operation(summary = "Verificar código TOTP")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Código válido"),
        @ApiResponse(responseCode = "401", description = "Código OTP inválido ou expirado")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/verificar")
    public ResponseEntity<String> verificar(@PathVariable Long id, @RequestBody TotpDTO dto) {
        boolean valido = totpService.verificar(id, dto.codigoOtp());
        if (valido) {
            return ResponseEntity.ok("{\"mensagem\": \"Código válido. Acesso permitido.\"}");
        }
        return ResponseEntity.status(401).body("{\"erro\": \"Código OTP inválido ou expirado.\"}");
    }
}
