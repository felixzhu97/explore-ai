package com.ai.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebCorsConfig")
class WebCorsConfigTest {

    private CorsProperties corsProperties;
    private WebCorsConfig webCorsConfig;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
        webCorsConfig = new WebCorsConfig(corsProperties);
    }

    @Nested
    @DisplayName("addCorsMappings")
    class AddCorsMappings {

        @Test
        @DisplayName("should allow configured production origins for api routes")
        void should_allowConfiguredProductionOrigins_when_registeringApiCorsMappings() {
            corsProperties.setAllowedOriginPatterns(List.of(
                    " https://explore-ai-git-*-felixzhu97s-projects.vercel.app ",
                    "https://www.felixzhu.chat",
                    "https://felixzhu.chat"
            ));

            Map<String, CorsConfiguration> corsConfigurations = registerCorsConfigurations();

            assertThat(corsConfigurations).containsOnlyKeys("/api/**");
            CorsConfiguration apiCorsConfiguration = corsConfigurations.get("/api/**");
            assertThat(apiCorsConfiguration.getAllowedOriginPatterns()).containsExactly(
                    "https://explore-ai-git-*-felixzhu97s-projects.vercel.app",
                    "https://www.felixzhu.chat",
                    "https://felixzhu.chat"
            );
            assertThat(apiCorsConfiguration.getAllowedMethods()).containsExactly(
                    "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
            );
            assertThat(apiCorsConfiguration.getAllowedHeaders()).containsExactly("*");
            assertThat(apiCorsConfiguration.getAllowCredentials()).isTrue();
            assertThat(apiCorsConfiguration.getMaxAge()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("should skip api cors mapping when origins are empty")
        void should_skipApiCorsMapping_when_originsAreEmpty() {
            corsProperties.setAllowedOriginPatterns(List.of());

            assertThat(registerCorsConfigurations()).isEmpty();
        }

        @Test
        @DisplayName("should skip api cors mapping when origins are null")
        void should_skipApiCorsMapping_when_originsAreNull() {
            corsProperties.setAllowedOriginPatterns(null);

            assertThat(registerCorsConfigurations()).isEmpty();
        }
    }

    private Map<String, CorsConfiguration> registerCorsConfigurations() {
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        webCorsConfig.addCorsMappings(registry);
        return registry.configurations();
    }

    private static class InspectableCorsRegistry extends CorsRegistry {

        private Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}
