package com.ai.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Log helpers that truncate content and hash identifiers to limit privacy leakage.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html">OWASP Logging</a>
 */
public final class LogSanitizer {

    private static final int DEFAULT_MAX_LENGTH = 50;

    private LogSanitizer() {
    }

    public static String truncate(String text) {
        return truncate(text, DEFAULT_MAX_LENGTH);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * One-way short fingerprint for UUIDs / client ids (not reversible).
     */
    public static String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            return "redacted";
        }
    }
}
