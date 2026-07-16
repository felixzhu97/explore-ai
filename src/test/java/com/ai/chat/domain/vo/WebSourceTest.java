package com.ai.chat.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSource")
class WebSourceTest {

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("should_normalizeNullFields_when_created")
        void should_normalizeNullFields_when_created() {
            WebSource source = new WebSource(null, null, null);

            assertThat(source.title()).isEmpty();
            assertThat(source.url()).isEmpty();
            assertThat(source.snippet()).isEmpty();
        }
    }

    @Nested
    @DisplayName("content hash")
    class ContentHashing {

        @Test
        @DisplayName("should_returnStableSha256_when_contentHasKnownValue")
        void should_returnStableSha256_when_contentHasKnownValue() {
            String hash = ContentHash.sha256("hello");

            assertThat(hash)
                    .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
            assertThat(ContentHash.sha256("hello")).isEqualTo(hash);
        }

        @Test
        @DisplayName("should_hashUnicodeWithUtf8_when_contentContainsMultilingualText")
        void should_hashUnicodeWithUtf8_when_contentContainsMultilingualText() {
            String hash = ContentHash.sha256("Paris 巴黎");

            assertThat(hash)
                    .isEqualTo("67a22c89dcdf4f971faf6dbeb20ff87701daf60bfd97e0d170377bd00538f5ec");
        }
    }
}
