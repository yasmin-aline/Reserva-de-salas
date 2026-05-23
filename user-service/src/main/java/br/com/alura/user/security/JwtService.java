package br.com.alura.user.security;

import br.com.alura.user.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.seconds}")
    private long expirationSeconds;

    @Value("${jwt.preauth.expiration.seconds}")
    private long preAuthExpirationSeconds;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("role", usuario.getRole())
                .claim("nome", usuario.getNome())
                .claim("email", usuario.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generatePreAuthToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("pending_2fa", true)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + preAuthExpirationSeconds * 1000))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public Object extractClaim(String token, String claimName) {
        return extractClaims(token).get(claimName);
    }

    public Date extractExpiry(String token) {
        return extractClaims(token).getExpiration();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
