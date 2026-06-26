package com.ai.chat.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("should create error response with message and error code")
        void shouldCreateErrorResponse_withMessageAndErrorCode() {
            // Given
            String message = "Resource not found";
            String errorCode = "NOT_FOUND";

            // When
            ErrorResponse response = ErrorResponse.of(message, errorCode);

            // Then
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.errorCode()).isEqualTo(errorCode);
            assertThat(response.errorCode()).isEqualTo(errorCode);
            assertThat(response.timestamp()).isNotNull();
            assertThat(response.path()).isNull();
        }

        @Test
        @DisplayName("should create error response with message, error code and path")
        void shouldCreateErrorResponse_withMessageErrorCodeAndPath() {
            // Given
            String message = "Invalid request";
            String errorCode = "BAD_REQUEST";
            String path = "/api/users/123";

            // When
            ErrorResponse response = ErrorResponse.of(message, errorCode, path);

            // Then
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.errorCode()).isEqualTo(errorCode);
            assertThat(response.errorCode()).isEqualTo(errorCode);
            assertThat(response.path()).isEqualTo(path);
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Field Value Tests")
    class FieldValueTests {

        @Test
        @DisplayName("should set error field to same value as error code")
        void shouldSetErrorField_toSameValueAsErrorCode() {
            // Given
            String errorCode = "VALIDATION_ERROR";

            // When
            ErrorResponse response = ErrorResponse.of("Validation failed", errorCode);

            // Then
            assertThat(response.errorCode()).isEqualTo(response.errorCode());
            assertThat(response.errorCode()).isEqualTo(errorCode);
        }

        @Test
        @DisplayName("should set timestamp to current time")
        void shouldSetTimestamp_toCurrentTime() {
            // Given
            Instant before = Instant.now();

            // When
            ErrorResponse response = ErrorResponse.of("Test", "TEST_ERROR");

            // Then
            Instant after = Instant.now();
            assertThat(response.timestamp())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should have null path when not provided")
        void shouldHaveNullPath_whenNotProvided() {
            // When
            ErrorResponse response = ErrorResponse.of("Test", "TEST_ERROR");

            // Then
            assertThat(response.path()).isNull();
        }

        @Test
        @DisplayName("should preserve all fields correctly")
        void shouldPreserveAllFields_correctly() {
            // Given
            String error = "INTERNAL_ERROR";
            String message = "An internal error occurred";
            String errorCode = "INTERNAL_ERROR";
            String path = "/api/v1/resource";

            // When
            ErrorResponse response = ErrorResponse.of(message, errorCode, path);

            // Then
            assertThat(response.errorCode()).isEqualTo(error);
            assertThat(response.message()).isEqualTo(message);
            assertThat(response.errorCode()).isEqualTo(errorCode);
            assertThat(response.path()).isEqualTo(path);
        }
    }

    @Nested
    @DisplayName("Common Error Code Scenarios")
    class CommonErrorCodeScenarios {

        @Test
        @DisplayName("should handle NOT_FOUND error code")
        void shouldHandleNotFoundErrorCode() {
            // When
            ErrorResponse response = ErrorResponse.of("User not found", "NOT_FOUND", "/api/users/999");

            // Then
            assertThat(response.errorCode()).isEqualTo("NOT_FOUND");
            assertThat(response.message()).contains("not found");
        }

        @Test
        @DisplayName("should handle VALIDATION_ERROR error code")
        void shouldHandleValidationErrorCode() {
            // When
            ErrorResponse response = ErrorResponse.of("Invalid email format", "VALIDATION_ERROR");

            // Then
            assertThat(response.errorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.message()).contains("Invalid");
        }

        @Test
        @DisplayName("should handle UNAUTHORIZED error code")
        void shouldHandleUnauthorizedErrorCode() {
            // When
            ErrorResponse response = ErrorResponse.of("Authentication required", "UNAUTHORIZED", "/api/protected");

            // Then
            assertThat(response.errorCode()).isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("should handle FORBIDDEN error code")
        void shouldHandleForbiddenErrorCode() {
            // When
            ErrorResponse response = ErrorResponse.of("Access denied", "FORBIDDEN");

            // Then
            assertThat(response.errorCode()).isEqualTo("FORBIDDEN");
        }

        @Test
        @DisplayName("should handle INTERNAL_SERVER_ERROR error code")
        void shouldHandleInternalServerErrorCode() {
            // When
            ErrorResponse response = ErrorResponse.of("Something went wrong", "INTERNAL_SERVER_ERROR");

            // Then
            assertThat(response.errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        }
    }
}
