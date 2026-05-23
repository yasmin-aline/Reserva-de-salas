package br.com.alura.booking.integration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.util.Date;

class JwtTestHelper {

    private static final String SECRET =
            "mySecretKeyForJWTThatIsAtLeast256BitsLongForHS256Algorithm12345";

    static String tokenFor(Long userId, String role) {
        try {
            MACSigner signer = new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8));
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("role", role)
                    .claim("nome", "Teste")
                    .claim("email", "teste@test.com")
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3_600_000L))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar JWT de teste", e);
        }
    }
}
