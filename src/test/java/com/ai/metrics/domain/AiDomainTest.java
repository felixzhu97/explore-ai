package com.ai.metrics.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AiDomain")
class AiDomainTest {

    @Test
    @DisplayName("should_parse_known_domain_when_case_insensitive")
    void should_parse_known_domain_when_case_insensitive() {
        assertThat(AiDomain.parse("CHAT")).contains(AiDomain.CHAT);
        assertThat(AiDomain.parse(" rag ")).contains(AiDomain.RAG);
    }

    @Test
    @DisplayName("should_return_empty_when_domain_blank_or_unknown")
    void should_return_empty_when_domain_blank_or_unknown() {
        assertThat(AiDomain.parse(null)).isEmpty();
        assertThat(AiDomain.parse("   ")).isEmpty();
        assertThat(AiDomain.parse("unknown")).isEmpty();
    }

    @Test
    @DisplayName("should_throw_when_require_unknown_domain")
    void should_throw_when_require_unknown_domain() {
        assertThatThrownBy(() -> AiDomain.require("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown AI domain");
    }
}
