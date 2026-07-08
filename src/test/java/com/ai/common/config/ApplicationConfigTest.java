package com.ai.common.config;

import com.ai.chat.domain.service.LanguageDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplicationConfig Tests")
class ApplicationConfigTest {

    private final AnnotationConfigApplicationContext context;

    ApplicationConfigTest() {
        this.context = new AnnotationConfigApplicationContext(ApplicationConfig.class);
    }

    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {

        @Test
        @DisplayName("should create LanguageDetectionService bean")
        void shouldCreateLanguageDetectionServiceBean() {
            LanguageDetectionService service = context.getBean(LanguageDetectionService.class);

            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("should create single instance of LanguageDetectionService")
        void shouldCreateSingleInstance_ofLanguageDetectionService() {
            LanguageDetectionService service1 = context.getBean(LanguageDetectionService.class);
            LanguageDetectionService service2 = context.getBean(LanguageDetectionService.class);

            assertThat(service1).isSameAs(service2);
        }
    }
}
