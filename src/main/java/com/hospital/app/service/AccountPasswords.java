package com.hospital.app.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class AccountPasswords {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);
    private AccountPasswords() {}

    public static boolean isEncoded(String value) {
        return value != null && value.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    public static boolean matches(String supplied, String stored) {
        if (supplied == null || stored == null) return false;
        if (isEncoded(stored)) return ENCODER.matches(supplied, stored);
        return MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }

    public static String encode(String password) { return ENCODER.encode(password); }
}
