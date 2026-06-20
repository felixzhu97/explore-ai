package com.ai.modules.rag.application.usecase;

import com.ai.modules.rag.application.usecase.DocumentUploadUseCase;
import com.ai.modules.rag.infrastructure.parser.PdfTextExtractor;
import com.ai.modules.rag.application.usecase.RagApplicationService;
import com.ai.modules.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentUploadUseCase")
class DocumentUploadUseCaseTest {

    @Mock
    private RagApplicationService ragApplicationService;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private MultipartFile multipartFile;

    private DocumentUploadUseCase documentUploadUseCase;

    @BeforeEach
    void setUp() {
        documentUploadUseCase = new DocumentUploadUseCase(ragApplicationService, pdfTextExtractor);
    }

    @Nested
    @DisplayName("upload()")
    class Upload {

        @Test
        @DisplayName("should upload PDF file successfully")
        void shouldUploadPdfFileSuccessfully() throws IOException {
            // Arrange
            String fileName = "document.pdf";
            String title = "My PDF Document";
            byte[] fileContent = "%PDF-1.4 content".getBytes();
            String extractedText = "Extracted PDF text content";

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(multipartFile.getSize()).thenReturn((long) fileContent.length);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("pdf");
            when(pdfTextExtractor.extractText(fileContent)).thenReturn(Optional.of(extractedText));
            when(ragApplicationService.uploadDocument(eq(title), eq(fileName), anyLong(), eq(extractedText)))
                    .thenReturn(new RagApplicationService.UploadResult(DocumentId.generate(), title, "READY", 3));

            // Act
            DocumentUploadUseCase.UploadResult result = documentUploadUseCase.upload(multipartFile, title);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo(title);
            assertThat(result.status()).isEqualTo("READY");
            assertThat(result.chunkCount()).isEqualTo(3);

            verify(pdfTextExtractor).extractText(fileContent);
        }

        @Test
        @DisplayName("should upload non-PDF file successfully")
        void shouldUploadNonPdfFileSuccessfully() throws IOException {
            // Arrange
            String fileName = "readme.txt";
            String title = "README";
            byte[] fileContent = "Hello, this is a text file content".getBytes();

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(multipartFile.getSize()).thenReturn((long) fileContent.length);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("txt");
            when(ragApplicationService.uploadDocument(eq(title), eq(fileName), anyLong(), anyString()))
                    .thenReturn(new RagApplicationService.UploadResult(DocumentId.generate(), title, "READY", 1));

            // Act
            DocumentUploadUseCase.UploadResult result = documentUploadUseCase.upload(multipartFile, title);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("READY");

            verify(pdfTextExtractor, never()).extractText(any());
        }

        @Test
        @DisplayName("should use custom title when provided")
        void shouldUseCustomTitleWhenProvided() throws IOException {
            // Arrange
            String fileName = "document.txt";
            String customTitle = "Custom Document Title";
            byte[] fileContent = "Content".getBytes();

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("txt");
            when(ragApplicationService.uploadDocument(eq(customTitle), eq(fileName), anyLong(), anyString()))
                    .thenReturn(new RagApplicationService.UploadResult(DocumentId.generate(), customTitle, "READY", 1));

            // Act
            DocumentUploadUseCase.UploadResult result = documentUploadUseCase.upload(multipartFile, customTitle);

            // Assert
            assertThat(result.title()).isEqualTo(customTitle);
        }

        @Test
        @DisplayName("should use filename as title when title is null")
        void shouldUseFilenameAsTitleWhenTitleIsNull() throws IOException {
            // Arrange
            String fileName = "my-document.txt";
            byte[] fileContent = "Content".getBytes();

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("txt");
            when(ragApplicationService.uploadDocument(eq(fileName), eq(fileName), anyLong(), anyString()))
                    .thenReturn(new RagApplicationService.UploadResult(DocumentId.generate(), fileName, "READY", 1));

            // Act
            DocumentUploadUseCase.UploadResult result = documentUploadUseCase.upload(multipartFile, null);

            // Assert
            assertThat(result.title()).isEqualTo(fileName);
        }

        @Test
        @DisplayName("should use filename as title when title is blank")
        void shouldUseFilenameAsTitleWhenTitleIsBlank() throws IOException {
            // Arrange
            String fileName = "another-doc.txt";
            byte[] fileContent = "Content".getBytes();

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("txt");
            when(ragApplicationService.uploadDocument(eq(fileName), eq(fileName), anyLong(), anyString()))
                    .thenReturn(new RagApplicationService.UploadResult(DocumentId.generate(), fileName, "READY", 1));

            // Act
            DocumentUploadUseCase.UploadResult result = documentUploadUseCase.upload(multipartFile, "   ");

            // Assert
            assertThat(result.title()).isEqualTo(fileName);
        }

        @Test
        @DisplayName("should throw DocumentUploadException when file read fails")
        void shouldThrowDocumentUploadExceptionWhenFileReadFails() throws IOException {
            // Arrange
            String fileName = "document.pdf";
            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenThrow(new IOException("File read error"));

            // Act & Assert
            assertThatThrownBy(() -> documentUploadUseCase.upload(multipartFile, "Title"))
                    .isInstanceOf(DocumentUploadUseCase.DocumentUploadException.class)
                    .hasMessageContaining("Failed to read file content");
        }

        @Test
        @DisplayName("should throw DocumentUploadException when PDF extraction fails")
        void shouldThrowDocumentUploadExceptionWhenPdfExtractionFails() throws IOException {
            // Arrange
            String fileName = "document.pdf";
            byte[] fileContent = "%PDF-1.4".getBytes();

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("pdf");
            when(pdfTextExtractor.extractText(fileContent)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentUploadUseCase.upload(multipartFile, "Title"))
                    .isInstanceOf(DocumentUploadUseCase.DocumentUploadException.class)
                    .hasMessageContaining("Failed to extract text from PDF");
        }
    }

    @Nested
    @DisplayName("extractContent()")
    class ExtractContent {

        @Test
        @DisplayName("should call pdfTextExtractor for PDF files")
        void shouldCallPdfTextExtractorForPdfFiles() throws IOException {
            // Arrange
            String fileName = "test.pdf";
            byte[] fileContent = "%PDF-1.4 fake pdf content".getBytes();
            String extractedText = "Extracted text from PDF";

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("pdf");
            when(pdfTextExtractor.extractText(fileContent)).thenReturn(Optional.of(extractedText));
            when(ragApplicationService.uploadDocument(anyString(), anyString(), anyLong(), eq(extractedText)))
                    .thenReturn(new RagApplicationService.UploadResult(DocumentId.generate(), "title", "READY", 1));

            // Act
            documentUploadUseCase.upload(multipartFile, "title");

            // Assert
            verify(pdfTextExtractor).getExtension(fileName);
            verify(pdfTextExtractor).extractText(fileContent);
        }

        @Test
        @DisplayName("should directly convert bytes for non-PDF files")
        void shouldDirectlyConvertBytesForNonPdfFiles() throws IOException {
            // Arrange
            String fileName = "test.txt";
            byte[] fileContent = "Plain text content".getBytes();

            when(multipartFile.getOriginalFilename()).thenReturn(fileName);
            when(multipartFile.getBytes()).thenReturn(fileContent);
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("txt");
            when(ragApplicationService.uploadDocument(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(new RagApplicationService.UploadResult(DocumentId.generate(), "title", "READY", 1));

            // Act
            documentUploadUseCase.upload(multipartFile, "title");

            // Assert
            verify(pdfTextExtractor).getExtension(fileName);
            verify(pdfTextExtractor, never()).extractText(any());
        }
    }
}
