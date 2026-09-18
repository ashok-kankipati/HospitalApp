package com.hospital.app.service.billing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

public final class RazorpaySignatureVerifier {
    private RazorpaySignatureVerifier() {
    }

    public static String sign(String payload, String secret) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload is required");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Razorpay secret is required");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign Razorpay payload", e);
        }
    }

    public static boolean isValid(String payload, String receivedSignature, String secret) {
        if (payload == null || receivedSignature == null || secret == null || secret.isBlank()) {
            return false;
        }
        return sign(payload, secret).equalsIgnoreCase(receivedSignature);
    }
}
