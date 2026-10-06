package com.ai.common.infra.logging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Log helpers that truncate content and hash identifiers to limit privacy leakage.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html">OWASP
 *     Logging</a>
 */
public final class LogSanitizer {

  private static final int DEFAULT_MAX_LENGTH = 50;

  private LogSanitizer() {}

  /** Cuts text to the default log length. */
  public static String truncate(String text) {
    return truncate(text, DEFAULT_MAX_LENGTH);
  }

  /** Cuts text to {@code maxLength} characters plus an ellipsis; renders null as "null". */
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
   * Returns the character count of user-supplied text, the only property of prompts and queries
   * that may be logged at INFO and above.
   */
  public static int lengthOf(String text) {
    return text == null ? 0 : text.length();
  }

  /**
   * One-way short fingerprint for high-entropy ids such as UUIDs. Do not use it for user text: the
   * unkeyed 32-bit hash of a short prompt can be recovered by guessing.
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
