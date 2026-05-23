package br.com.alura.user.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Criptografa totp_secret no banco usando AES-256-GCM.
 * Formato armazenado: Base64(IV[12] || ciphertext+tag).
 * Migração: se o valor não for Base64 válido ou falhar a descriptografia,
 * retorna null — o usuário precisará reconfigurar o 2FA.
 */
@Converter
public class TotpSecretConverter implements AttributeConverter<String, String> {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(TotpEncryptionKeyHolder.getKeyBytes(), "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao criptografar totp_secret", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            byte[] iv = Arrays.copyOf(decoded, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(decoded, IV_LENGTH, decoded.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(TotpEncryptionKeyHolder.getKeyBytes(), "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // dado legado em texto puro — invalida o vínculo, usuário reconfigurar 2FA
            return null;
        }
    }
}
