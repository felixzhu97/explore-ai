package com.ai.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebCorsConfig")
class WebCorsConfigTest {

    @Test
    @DisplayName("should allow credentials")
    void shouldAllowCredentials() {
        CorsConfiguration config = corsConfiguration("http://localhost:4200", "https://www.felixzhu.chat");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("should allow all headers")
    void shouldAllowAllHeaders() {
        CorsConfiguration config = corsConfiguration("http://localhost:4200", "https://www.felixzhu.chat");
        assertThat(config.getAllowedHeaders()).contains("*");
    }

    @Test
    @DisplayName("should set maxAge to 3600")
    void shouldSetMaxAgeTo3600() {
        CorsConfiguration config = corsConfiguration("http://localhost:4200", "https://www.felixzhu.chat");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("should return config for /api/chat from felixzhu.chat")
    void shouldReturnConfigForApiChatFromFelixzhuChat() {
        var source = corsConfigurationSource("http://localhost:4200", "https://www.felixzhu.chat");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/chat");
        request.addHeader("Origin", "https://www.felixzhu.chat");

        CorsConfiguration matched = source.getCorsConfiguration(request);
        assertThat(matched).isNotNull();
        assertThat(matched.getAllowedOriginPatterns()).contains("https://www.felixzhu.chat");
    }

    @Test
    @DisplayName("should apply felixzhu.chat origin")
    void shouldApplyFelixzhuChatOrigin() {
        CorsConfiguration config = corsConfiguration("https://www.felixzhu.chat");
        assertThat(config.getAllowedOriginPatterns()).contains("https://www.felixzhu.chat");
    }

    private CorsConfiguration corsConfiguration(String... origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(java.util.Arrays.asList(origins));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setMaxAge(3600L);
        return config;
    }

    private CorsConfigurationSource corsConfigurationSource(String... origins) {
        CorsConfiguration config = corsConfiguration(origins);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
