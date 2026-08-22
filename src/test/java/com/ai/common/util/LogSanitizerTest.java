package com.ai.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LogSanitizer")
class LogSanitizerTest {

  @Test
  void shouldReturnStableFingerprintWhenSameInput() {
    String a = LogSanitizer.fingerprint("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    String b = LogSanitizer.fingerprint("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    assertThat(a).isEqualTo(b).hasSize(8);
  }

  @Test
  void shouldDifferFingerprintWhenInputDiffers() {
    assertThat(LogSanitizer.fingerprint("client-a"))
        .isNotEqualTo(LogSanitizer.fingerprint("client-b"));
  }

  @Test
  void shouldTruncateLongText() {
    assertThat(LogSanitizer.truncate("x".repeat(80), 10)).isEqualTo("xxxxxxxxxx...");
  }
}
