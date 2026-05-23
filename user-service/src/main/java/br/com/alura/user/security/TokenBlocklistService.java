package br.com.alura.user.security;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocklist de tokens JWT em memória.
 * Tokens revogados são armazenados pelo hash SHA-256 até sua expiração natural.
 * Nota: dados perdidos no restart — para produção, use Redis.
 */
@Service
public class TokenBlocklistService {

    private final ConcurrentHashMap<String, Long> blocklist = new ConcurrentHashMap<>();

    public void blacklist(String token, Date expiry) {
        blocklist.put(hash(token), expiry.getTime());
        purgeExpired();
    }

    public boolean isBlacklisted(String token) {
        purgeExpired();
        return blocklist.containsKey(hash(token));
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        blocklist.entrySet().removeIf(e -> e.getValue() < now);
    }

    private String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(token.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
