package br.com.alura.user.security;

import br.com.alura.user.model.Usuario;
import br.com.alura.user.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public OAuth2SuccessHandler(UsuarioRepository usuarioRepository, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String nome = oauth2User.getAttribute("name");

        String sub = oauth2User.getAttribute("sub");

        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario novo = new Usuario();
            novo.setNome(nome != null ? nome : email);
            novo.setEmail(email);
            novo.setRole("USER");
            novo.setProviderType("GOOGLE");
            novo.setProviderId(sub);
            return usuarioRepository.save(novo);
        });

        // Atualiza vínculo Google se ainda não gravado (usuário criado localmente antes)
        if (usuario.getProviderId() == null && sub != null) {
            usuario.setProviderId(sub);
            usuario.setProviderType("GOOGLE");
            usuarioRepository.save(usuario);
        }

        if (usuario.isTotpAtivo()) {
            String preAuthToken = jwtService.generatePreAuthToken(usuario.getId());
            response.sendRedirect(frontendUrl + "/2fa?token=" + preAuthToken);
            return;
        }

        String token = jwtService.generateToken(usuario);
        response.sendRedirect(frontendUrl + "?token=" + token);
    }
}
