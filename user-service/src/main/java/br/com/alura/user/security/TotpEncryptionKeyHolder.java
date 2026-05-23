package br.com.alura.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class TotpEncryptionKeyHolder {

    private static byte[] keyBytes;

    @Value("${totp.encryption.key}")
    public void init(String key) {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        keyBytes = Arrays.copyOf(raw, 32); // AES-256: sempre 32 bytes
    }

    public static byte[] getKeyBytes() {
        return keyBytes;
    }
}
