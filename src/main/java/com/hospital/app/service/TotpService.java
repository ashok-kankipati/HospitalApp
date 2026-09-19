package com.hospital.app.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class TotpService {
    private static final int PERIOD_SECONDS = 30;
    private static final int MAX_CLOCK_DRIFT_WINDOWS = 1;
    private static final int RECOVERY_CODE_COUNT = 8;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    private final CodeGenerator codes = new DefaultCodeGenerator();

    @Value("${hospital.totp.enabled:false}")
    private boolean enabled;

    @Value("${hospital.totp.issuer:CareFlow}")
    private String issuer;

    @Value("${hospital.totp.encryption-key:}")
    private String encryptionKey;

    public TotpService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void initialize() {
        if (!enabled) return;
        encryptionKeyBytes();
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS user_totp_mfa (
                    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                    encrypted_secret TEXT NOT NULL,
                    recovery_codes TEXT,
                    last_used_counter BIGINT,
                    enabled BOOLEAN NOT NULL DEFAULT false,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEnrolled(Long userId) {
        if (!enabled || userId == null) return false;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_totp_mfa WHERE user_id = ? AND enabled = true",
                Integer.class, userId);
        return count != null && count > 0;
    }

    public Enrollment beginEnrollment(String username) {
        String secret = new DefaultSecretGenerator(32).generate();
        String uri = "otpauth://totp/" + encode(issuer) + ":" + encode(username)
                + "?secret=" + encode(secret) + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=6&period=" + PERIOD_SECONDS;
        return new Enrollment(secret, qrDataUrl(uri), secret.replaceAll("(.{4})", "$1 ").trim());
    }

    public List<String> enroll(Long userId, String secret, String code) {
        long counter = verifiedCounter(secret, code);
        if (counter < 0) throw new IllegalArgumentException("The verification code is invalid or expired.");
        List<String> recoveryCodes = generateRecoveryCodes();
        String hashes = recoveryCodes.stream().map(AccountPasswords::encode).reduce((a, b) -> a + "\n" + b).orElse("");
        jdbc.update("""
                INSERT INTO user_totp_mfa (user_id, encrypted_secret, recovery_codes, last_used_counter, enabled, updated_at)
                VALUES (?, ?, ?, ?, true, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id) DO UPDATE SET encrypted_secret = EXCLUDED.encrypted_secret,
                    recovery_codes = EXCLUDED.recovery_codes, last_used_counter = EXCLUDED.last_used_counter,
                    enabled = true, updated_at = CURRENT_TIMESTAMP
                """, userId, encrypt(secret), hashes, counter);
        return recoveryCodes;
    }

    public boolean verify(Long userId, String suppliedCode) {
        if (userId == null || suppliedCode == null || suppliedCode.isBlank()) return false;
        List<Credential> credentials = jdbc.query(
                "SELECT encrypted_secret, recovery_codes, last_used_counter FROM user_totp_mfa WHERE user_id = ? AND enabled = true",
                (rs, row) -> new Credential(rs.getString(1), rs.getString(2), (Long) rs.getObject(3)), userId);
        if (credentials.isEmpty()) return false;
        Credential credential = credentials.get(0);
    String normalized = suppliedCode.replace(" ", "").trim();
        if (normalized.matches("\\d{6}")) {
            long counter = verifiedCounter(decrypt(credential.encryptedSecret()), normalized);
            if (counter < 0 || (credential.lastUsedCounter() != null && counter <= credential.lastUsedCounter())) return false;
            jdbc.update("UPDATE user_totp_mfa SET last_used_counter = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                    counter, userId);
            return true;
        }
        return consumeRecoveryCode(userId, normalized, credential.recoveryCodes());
    }

    public void reset(Long userId) {
        if (userId != null) jdbc.update("DELETE FROM user_totp_mfa WHERE user_id = ?", userId);
    }

    private long verifiedCounter(String secret, String suppliedCode) {
        long current = Instant.now().getEpochSecond() / PERIOD_SECONDS;
        for (long counter = current - MAX_CLOCK_DRIFT_WINDOWS; counter <= current + MAX_CLOCK_DRIFT_WINDOWS; counter++) {
            try {
                String expected = codes.generate(secret, counter);
                if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), suppliedCode.getBytes(StandardCharsets.UTF_8))) {
                    return counter;
                }
            } catch (Exception ignored) {
                return -1;
            }
        }
        return -1;
    }

    private boolean consumeRecoveryCode(Long userId, String supplied, String stored) {
        if (stored == null || supplied.length() < 8) return false;
        List<String> remaining = new ArrayList<>(List.of(stored.split("\\n")));
        for (int index = 0; index < remaining.size(); index++) {
            if (AccountPasswords.matches(supplied.toUpperCase(), remaining.get(index))) {
                remaining.remove(index);
                jdbc.update("UPDATE user_totp_mfa SET recovery_codes = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                        String.join("\n", remaining), userId);
                return true;
            }
        }
        return false;
    }

    private List<String> generateRecoveryCodes() {
        List<String> result = new ArrayList<>();
        for (int index = 0; index < RECOVERY_CODE_COUNT; index++) {
            byte[] bytes = new byte[6];
            random.nextBytes(bytes);
            result.add(java.util.HexFormat.of().formatHex(bytes).toUpperCase());
        }
        return result;
    }

    private String encrypt(String value) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKeyBytes(), "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not protect the authenticator secret.", exception);
        }
    }

    private String decrypt(String value) {
        try {
            byte[] combined = Base64.getDecoder().decode(value);
            byte[] nonce = java.util.Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKeyBytes(), "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read the authenticator secret.", exception);
        }
    }

    private byte[] encryptionKeyBytes() {
        try {
            byte[] key = Base64.getDecoder().decode(encryptionKey == null ? "" : encryptionKey.trim());
            if (key.length != 32) throw new IllegalArgumentException();
            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MFA_ENCRYPTION_KEY must be a Base64-encoded 32-byte key.");
        }
    }

    private String qrDataUrl(String value) {
        try {
            BitMatrix matrix = new com.google.zxing.qrcode.QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 280, 280);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate the authenticator QR code.", exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record Enrollment(String secret, String qrCode, String manualKey) {}
    private record Credential(String encryptedSecret, String recoveryCodes, Long lastUsedCounter) {}
}
