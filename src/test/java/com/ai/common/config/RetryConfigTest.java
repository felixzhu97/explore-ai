package com.ai.common.config;

import com.ai.common.config.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RetryConfig")
class RetryConfigTest {

    private RetryTemplate retryTemplate;

    @BeforeEach
    void setUp() {
        RetryConfig config = new RetryConfig();
        retryTemplate = config.aiRetryTemplate();
    }

    @Nested
    @DisplayName("aiRetryTemplate()")
    class AiRetryTemplate {

        @Test
        @DisplayName("should retry on recoverable failures")
        void shouldRetryOnRecoverableFailures() {
            int[] counter = {0};

            String result = retryTemplate.execute(context -> {
                counter[0]++;
                if (counter[0] < 3) {
                    throw new RuntimeException("Recoverable");
                }
                return "Success";
            });

            assertThat(result).isEqualTo("Success");
            assertThat(counter[0]).isEqualTo(3);
        }

        @Test
        @DisplayName("should succeed on first attempt")
        void shouldSucceedOnFirstAttempt() {
            int[] counter = {0};

            String result = retryTemplate.execute(context -> {
                counter[0]++;
                return "Immediate success";
            });

            assertThat(result).isEqualTo("Immediate success");
            assertThat(counter[0]).isEqualTo(1);
        }

        @Test
        @DisplayName("should fail after max attempts on persistent failures")
        void shouldFailAfterMaxAttemptsOnPersistentFailures() {
            int[] counter = {0};

            assertThatThrownBy(() -> retryTemplate.execute(context -> {
                counter[0]++;
                throw new RuntimeException("Persistent failure");
            }))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Persistent failure");

            assertThat(counter[0]).isEqualTo(3);
        }

        @Test
        @DisplayName("should propagate checked exceptions")
        void shouldPropagateCheckedExceptions() {
            assertThatThrownBy(() -> retryTemplate.execute(context -> {
                throw new Exception("Checked exception");
            }))
                    .isInstanceOf(Exception.class)
                    .hasMessage("Checked exception");
        }
    }
}
