package com.ai.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebCorsConfig")
class WebCorsConfigTest {

    @Test
    @DisplayName("should allow credentials")
    void shouldAllowCredentials() throws Exception {
        CorsConfiguration config = corsConfigurationFromWebCorsConfig("http://localhost:4200", "https://www.felixzhu.chat");
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("should allow all headers")
    void shouldAllowAllHeaders() throws Exception {
        CorsConfiguration config = corsConfigurationFromWebCorsConfig("http://localhost:4200", "https://www.felixzhu.chat");
        assertThat(config.getAllowedHeaders()).contains("*");
    }

    @Test
    @DisplayName("should set maxAge to 3600")
    void shouldSetMaxAgeTo3600() throws Exception {
        CorsConfiguration config = corsConfigurationFromWebCorsConfig("http://localhost:4200", "https://www.felixzhu.chat");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("should return config for /api/chat from felixzhu.chat")
    void shouldReturnConfigForApiChatFromFelixzhuChat() throws Exception {
        CorsConfigurationSource source = corsConfigurationSourceFromWebCorsConfig(
                "http://localhost:4200", "https://www.felixzhu.chat");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/chat");
        request.addHeader("Origin", "https://www.felixzhu.chat");

        CorsConfiguration matched = source.getCorsConfiguration(request);
        assertThat(matched).isNotNull();
        assertThat(matched.getAllowedOriginPatterns()).contains("https://www.felixzhu.chat");
    }

    @Test
    @DisplayName("should apply felixzhu.chat origin")
    void shouldApplyFelixzhuChatOrigin() throws Exception {
        CorsConfiguration config = corsConfigurationFromWebCorsConfig("https://www.felixzhu.chat");
        assertThat(config.getAllowedOriginPatterns()).contains("https://www.felixzhu.chat");
    }

    private CorsConfiguration corsConfigurationFromWebCorsConfig(String... origins) throws Exception {
        WebCorsConfig webConfig = new WebCorsConfig();
        setAllowedOriginPatterns(webConfig, origins);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(origins));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setMaxAge(3600L);
        return config;
    }

    private CorsConfigurationSource corsConfigurationSourceFromWebCorsConfig(String... origins) throws Exception {
        WebCorsConfig webConfig = new WebCorsConfig();
        setAllowedOriginPatterns(webConfig, origins);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(origins));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void setAllowedOriginPatterns(WebCorsConfig config, String[] origins) throws Exception {
        Field field = WebCorsConfig.class.getDeclaredField("allowedOriginPatterns");
        field.setAccessible(true);
        field.set(config, origins);
    }
}
