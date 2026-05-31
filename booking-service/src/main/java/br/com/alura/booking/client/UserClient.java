package br.com.alura.booking.client;

import br.com.alura.booking.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public UserClient() {
        this.restTemplate = new RestTemplate();
    }

    private String buildJwt() throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis() / 1000;
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"1\",\"role\":\"ADMIN\",\"iat\":" + now + ",\"exp\":" + (now + 60) + "}").getBytes(StandardCharsets.UTF_8));
        String data = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        return data + "." + signature;
    }

    public void checkUser(Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + buildJwt());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    "http://localhost:8081/api/v1/usuarios/" + userId,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );
            if (response.getBody() == null) {
                throw new RegraDeNegocioException("Usuário não encontrado.");
            }
        } catch (RegraDeNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new RegraDeNegocioException("Erro ao comunicar com o User Service ou usuário inexistente.");
        }
    }
}