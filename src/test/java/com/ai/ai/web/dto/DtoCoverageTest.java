package com.ai.ai.web.dto;

import com.ai.rag.web.dto.RagChatResponse;
import com.ai.rag.web.dto.SourceDocumentDto;
import com.ai.rag.web.dto.UploadDocumentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DtoCoverageTest - Unit tests for DTOs with low coverage.
 *
 * Naming convention: should_expected_result_when_condition
 * Uses AAA pattern (Arrange-Act-Assert)
 * Tests record DTOs and their constructors/accessors.
 */
@DisplayName("DTO Coverage Tests")
class DtoCoverageTest {

    @Nested
    @DisplayName("SourceDocumentDto")
    class SourceDocumentDtoTests {

        @Test
        @DisplayName("should create SourceDocumentDto with all fields")
        void shouldCreateSourceDocumentDto() {
            // Arrange
            String id = "doc-123";
            String content = "This is the document content";
            float score = 0.95f;
            Map<String, Object> metadata = Map.of("source", "test.pdf", "page", 1);

            // Act
            SourceDocumentDto dto = new SourceDocumentDto(id, content, score, metadata);

            // Assert
            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.content()).isEqualTo(content);
            assertThat(dto.score()).isEqualTo(score);
            assertThat(dto.metadata()).isEqualTo(metadata);
        }

        @Test
        @DisplayName("should access SourceDocumentDto fields individually")
        void shouldAccessSourceDocumentDtoFields() {
            // Arrange
            String id = "test-id";
            String content = "Content text";
            float score = 0.5f;
            Map<String, Object> metadata = Map.of("key", "value");

            // Act
            SourceDocumentDto dto = new SourceDocumentDto(id, content, score, metadata);

            // Assert - test each accessor individually
            assertThat(dto.id()).isEqualTo("test-id");
            assertThat(dto.content()).isEqualTo("Content text");
            assertThat(dto.score()).isEqualTo(0.5f);
            assertThat(dto.metadata().get("key")).isEqualTo("value");
        }

        @Test
        @DisplayName("should handle zero score")
        void shouldHandleZeroScore() {
            // Act
            SourceDocumentDto dto = new SourceDocumentDto("id", "content", 0.0f, Map.of());

            // Assert
            assertThat(dto.score()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("should handle max score value")
        void shouldHandleMaxScoreValue() {
            // Act
            SourceDocumentDto dto = new SourceDocumentDto("id", "content", 1.0f, Map.of());

            // Assert
            assertThat(dto.score()).isEqualTo(1.0f);
        }
    }

    @Nested
    @DisplayName("UploadDocumentRequest")
    class UploadDocumentRequestTests {

        @Test
        @DisplayName("should create UploadDocumentRequest with all fields")
        void shouldCreateUploadDocumentRequest() {
            // Arrange
            String title = "Test Document";
            String content = "Document content here";
            String fileName = "test.pdf";

            // Act
            UploadDocumentRequest request = new UploadDocumentRequest(title, content, fileName);

            // Assert
            assertThat(request.title()).isEqualTo(title);
            assertThat(request.content()).isEqualTo(content);
            assertThat(request.fileName()).isEqualTo(fileName);
        }

        @Test
        @DisplayName("should create UploadDocumentRequest without fileName")
        void shouldCreateUploadDocumentRequestWithoutFileName() {
            // Act
            UploadDocumentRequest request = new UploadDocumentRequest("Title", "Content", null);

            // Assert
            assertThat(request.title()).isEqualTo("Title");
            assertThat(request.content()).isEqualTo("Content");
            assertThat(request.fileName()).isNull();
        }

        @Test
        @DisplayName("should handle empty title")
        void shouldHandleEmptyTitle() {
            // Act
            UploadDocumentRequest request = new UploadDocumentRequest("", "Content", null);

            // Assert
            assertThat(request.title()).isEmpty();
        }

        @Test
        @DisplayName("should handle empty content")
        void shouldHandleEmptyContent() {
            // Act
            UploadDocumentRequest request = new UploadDocumentRequest("Title", "", null);

            // Assert
            assertThat(request.content()).isEmpty();
        }

        @Test
        @DisplayName("should handle unicode characters in fields")
        void shouldHandleUnicodeCharactersInFields() {
            // Act
            UploadDocumentRequest request = new UploadDocumentRequest("标题文档", "中文内容", "文件.pdf");

            // Assert
            assertThat(request.title()).isEqualTo("标题文档");
            assertThat(request.content()).isEqualTo("中文内容");
            assertThat(request.fileName()).isEqualTo("文件.pdf");
        }
    }

    @Nested
    @DisplayName("RagChatResponse")
    class RagChatResponseTests {

        @Test
        @DisplayName("should create RagChatResponse with content and sources")
        void shouldCreateRagChatResponse() {
            // Arrange
            String content = "AI generated response";
            List<SourceDocumentDto> sources = List.of(
                    new SourceDocumentDto("1", "Source 1", 0.9f, Map.of()),
                    new SourceDocumentDto("2", "Source 2", 0.8f, Map.of())
            );

            // Act
            RagChatResponse response = new RagChatResponse(content, sources);

            // Assert
            assertThat(response.content()).isEqualTo(content);
            assertThat(response.sources()).hasSize(2);
        }

        @Test
        @DisplayName("should create RagChatResponse with empty sources")
        void shouldCreateRagChatResponseWithEmptySources() {
            // Act
            RagChatResponse response = new RagChatResponse("Response text", List.of());

            // Assert
            assertThat(response.content()).isEqualTo("Response text");
            assertThat(response.sources()).isEmpty();
        }

        @Test
        @DisplayName("should create RagChatResponse with null sources")
        void shouldCreateRagChatResponseWithNullSources() {
            // Act
            RagChatResponse response = new RagChatResponse("Response", null);

            // Assert
            assertThat(response.content()).isEqualTo("Response");
            assertThat(response.sources()).isNull();
        }

        @Test
        @DisplayName("should handle empty content")
        void shouldHandleEmptyContent() {
            // Act
            RagChatResponse response = new RagChatResponse("", List.of());

            // Assert
            assertThat(response.content()).isEmpty();
        }

        @Test
        @DisplayName("should handle multiline content")
        void shouldHandleMultilineContent() {
            // Arrange
            String multilineContent = "Line 1\nLine 2\nLine 3";

            // Act
            RagChatResponse response = new RagChatResponse(multilineContent, List.of());

            // Assert
            assertThat(response.content()).contains("\n");
            assertThat(response.content().split("\n")).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Null value handling")
    class NullValueHandlingTests {

        @Test
        @DisplayName("should handle null values in SourceDocumentDto")
        void shouldHandleNullValuesInSourceDocumentDto() {
            // Act
            SourceDocumentDto dto = new SourceDocumentDto(null, null, 0.0f, null);

            // Assert
            assertThat(dto.id()).isNull();
            assertThat(dto.content()).isNull();
            assertThat(dto.metadata()).isNull();
        }

        @Test
        @DisplayName("should handle null values in UploadDocumentRequest")
        void shouldHandleNullValuesInUploadDocumentRequest() {
            // Act
            UploadDocumentRequest request = new UploadDocumentRequest(null, null, null);

            // Assert
            assertThat(request.title()).isNull();
            assertThat(request.content()).isNull();
            assertThat(request.fileName()).isNull();
        }

        @Test
        @DisplayName("should handle null content in RagChatResponse")
        void shouldHandleNullContentInRagChatResponse() {
            // Act
            RagChatResponse response = new RagChatResponse(null, List.of());

            // Assert
            assertThat(response.content()).isNull();
        }
    }

    @Nested
    @DisplayName("Record equals and hashCode")
    class RecordEqualsHashCodeTests {

        @Test
        @DisplayName("SourceDocumentDto should have correct equals behavior")
        void sourceDocumentDtoShouldHaveCorrectEqualsBehavior() {
            // Arrange
            SourceDocumentDto dto1 = new SourceDocumentDto("id", "content", 0.5f, Map.of("key", "value"));
            SourceDocumentDto dto2 = new SourceDocumentDto("id", "content", 0.5f, Map.of("key", "value"));
            SourceDocumentDto dto3 = new SourceDocumentDto("different-id", "content", 0.5f, Map.of());

            // Assert
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1).isNotEqualTo(dto3);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("UploadDocumentRequest should have correct equals behavior")
        void uploadDocumentRequestShouldHaveCorrectEqualsBehavior() {
            // Arrange
            UploadDocumentRequest req1 = new UploadDocumentRequest("Title", "Content", "file.pdf");
            UploadDocumentRequest req2 = new UploadDocumentRequest("Title", "Content", "file.pdf");
            UploadDocumentRequest req3 = new UploadDocumentRequest("Different", "Content", "file.pdf");

            // Assert
            assertThat(req1).isEqualTo(req2);
            assertThat(req1).isNotEqualTo(req3);
        }

        @Test
        @DisplayName("RagChatResponse should have correct equals behavior")
        void ragChatResponseShouldHaveCorrectEqualsBehavior() {
            // Arrange
            RagChatResponse resp1 = new RagChatResponse("Text", List.of());
            RagChatResponse resp2 = new RagChatResponse("Text", List.of());
            RagChatResponse resp3 = new RagChatResponse("Different", List.of());

            // Assert
            assertThat(resp1).isEqualTo(resp2);
            assertThat(resp1).isNotEqualTo(resp3);
        }
    }
}
