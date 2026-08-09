package com.ai.metrics.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AiDomain")
class AiDomainTest {

    @Test
    @DisplayName("should parse known domain when case insensitive")
    void shouldParseKnownDomainWhenCaseInsensitive() {
        assertThat(AiDomain.parse("CHAT")).contains(AiDomain.CHAT);
        assertThat(AiDomain.parse(" rag ")).contains(AiDomain.RAG);
    }

    @Test
    @DisplayName("should return empty when domain blank or unknown")
    void shouldReturnEmptyWhenDomainBlankOrUnknown() {
        assertThat(AiDomain.parse(null)).isEmpty();
        assertThat(AiDomain.parse("   ")).isEmpty();
        assertThat(AiDomain.parse("unknown")).isEmpty();
    }

    @Test
    @DisplayName("should throw when require unknown domain")
    void shouldThrowWhenRequireUnknownDomain() {
        assertThatThrownBy(() -> AiDomain.require("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown AI domain");
    }
}
