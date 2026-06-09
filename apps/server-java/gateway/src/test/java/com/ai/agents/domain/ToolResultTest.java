package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolResult Tests")
class ToolResultTest {

    @Nested
    @DisplayName("success factory method")
    class SuccessFactoryMethodTests {

        @Test
        @DisplayName("should create successful result with content")
        void shouldCreateSuccessfulResultWithContent() {
            ToolResult result = ToolResult.success("Operation completed successfully");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).isEqualTo("Operation completed successfully");
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should handle null content")
        void shouldHandleNullContent() {
            ToolResult result = ToolResult.success(null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.content()).isNull();
        }
    }

    @Nested
    @DisplayName("error factory method")
    class ErrorFactoryMethodTests {

        @Test
        @DisplayName("should create error result with message")
        void shouldCreateErrorResultWithMessage() {
            ToolResult result = ToolResult.error("Operation failed");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isError()).isTrue();
            assertThat(result.errorMessage()).isEqualTo("Operation failed");
            assertThat(result.content()).isNull();
        }
    }

    @Nested
    @DisplayName("getDisplayContent method")
    class GetDisplayContentMethodTests {

        @Test
        @DisplayName("should return content for successful result")
        void shouldReturnContentForSuccessfulResult() {
            ToolResult result = ToolResult.success("Operation result");

            assertThat(result.getDisplayContent()).isEqualTo("Operation result");
        }

        @Test
        @DisplayName("should return empty string for successful result with null content")
        void shouldReturnEmptyStringForSuccessfulResultWithNullContent() {
            ToolResult result = ToolResult.success(null);

            assertThat(result.getDisplayContent()).isEmpty();
        }

        @Test
        @DisplayName("should return error message prefixed with Error for error result")
        void shouldReturnErrorMessagePrefixedWithErrorForErrorResult() {
            ToolResult result = ToolResult.error("Something went wrong");

            assertThat(result.getDisplayContent()).isEqualTo("Error: Something went wrong");
        }

        @Test
        @DisplayName("should return Error: Unknown error for error result with null message")
        void shouldReturnErrorUnknownErrorForErrorResultWithNullMessage() {
            ToolResult result = ToolResult.error(null);

            assertThat(result.getDisplayContent()).isEqualTo("Error: Unknown error");
        }
    }

    @Nested
    @DisplayName("completedAt accessor")
    class CompletedAtAccessorTests {

        @Test
        @DisplayName("should set completedAt to now when not provided")
        void shouldSetCompletedAtToNowWhenNotProvided() {
            ToolResult result = ToolResult.success("Done");

            assertThat(result.completedAt()).isNotNull();
        }
    }
}
