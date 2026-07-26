package com.ai.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void should_returnStableFingerprint_whenSameInput() {
        String a = LogSanitizer.fingerprint("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        String b = LogSanitizer.fingerprint("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        assertThat(a).isEqualTo(b).hasSize(8);
    }

    @Test
    void should_differFingerprint_whenInputDiffers() {
        assertThat(LogSanitizer.fingerprint("client-a"))
                .isNotEqualTo(LogSanitizer.fingerprint("client-b"));
    }

    @Test
    void should_truncateLongText() {
        assertThat(LogSanitizer.truncate("x".repeat(80), 10)).isEqualTo("xxxxxxxxxx...");
    }
}
