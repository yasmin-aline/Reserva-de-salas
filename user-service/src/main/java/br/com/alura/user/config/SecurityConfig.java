package br.com.alura.user.config;

/*
 * AUTENTICAÇÃO IMPLEMENTADA: Google OAuth2 + 2FA TOTP local
 *
 * Opção escolhida: Google OAuth2 (spring-boot-starter-oauth2-client) para login social
 *                 + TOTP local (dev.samstevens.totp) para segundo fator.
 *
 * COMO REVERTER para Basic Auth:
 * 1. Em pom.xml: remover jjwt-*, oauth2-client, oauth2-resource-server, totp.
 * 2. Restaurar SecurityConfig: BasicAuth + InMemoryUserDetailsManager.
 * 3. Remover classes: JwtService, AuthController, OAuth2SuccessHandler,
 *    TotpSecretConverter, TotpEncryptionKeyHolder, TokenBlocklistService, JwtBlacklistFilter.
 * 4. Em Usuario: remover campos senha, role, providerType, providerId, totpSecret, totpAtivo.
 * 5. Em application.properties: remover blocos jwt.* e totp.encryption.key.
 */

import br.com.alura.user.security.JwtBlacklistFilter;
import br.com.alura.user.security.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final JwtBlacklistFilter jwtBlacklistFilter;

    public SecurityConfig(OAuth2SuccessHandler oauth2SuccessHandler,
                          JwtBlacklistFilter jwtBlacklistFilter) {
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.jwtBlacklistFilter = jwtBlacklistFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/oauth2/**", "/login/oauth2/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2.successHandler(oauth2SuccessHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**", "/v3/api-docs.yaml"
                ).permitAll()
                .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtBlacklistFilter, BearerTokenAuthenticationFilter.class)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"erro\": \"Não autenticado. Faça login para continuar.\"}");
                })
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"erro\": \"Acesso negado. Você não tem permissão para realizar esta operação.\"}");
                })
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) return List.of();
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
