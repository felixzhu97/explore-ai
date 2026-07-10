package com.ai.rag.infrastructure.etl;

import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdfAndTextDocumentReader")
class PdfAndTextDocumentReaderTest {

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    private PdfAndTextDocumentReader reader;

    @BeforeEach
    void setUp() {
        reader = new PdfAndTextDocumentReader(pdfTextExtractor);
    }

    @Test
    @DisplayName("should read plain text without invoking PDF extraction")
    void should_read_plain_text_without_invoking_pdf_extraction() {
        byte[] content = "plain text content".getBytes(StandardCharsets.UTF_8);
        when(pdfTextExtractor.getExtension("notes.txt")).thenReturn("txt");

        var document = reader.read(content, "notes.txt");

        assertThat(document.content()).isEqualTo("plain text content");
        assertThat(document.metadata()).containsEntry("fileName", "notes.txt");
        assertThat(document.source()).isEqualTo("notes.txt");
        verify(pdfTextExtractor, never()).extractText(content);
    }

    @Test
    @DisplayName("should extract PDF text for PDF extension ignoring case")
    void should_extract_pdf_text_for_pdf_extension_ignoring_case() {
        byte[] content = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        when(pdfTextExtractor.getExtension("Manual.PDF")).thenReturn("PDF");
        when(pdfTextExtractor.extractText(content)).thenReturn(Optional.of("extracted manual text"));

        var document = reader.read(content, "Manual.PDF");

        assertThat(document.content()).isEqualTo("extracted manual text");
        assertThat(document.metadata()).containsEntry("fileName", "Manual.PDF");
        assertThat(document.source()).isEqualTo("Manual.PDF");
        verify(pdfTextExtractor).extractText(content);
    }

    @Test
    @DisplayName("should fail when PDF extraction returns empty")
    void should_fail_when_pdf_extraction_returns_empty() {
        byte[] content = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        when(pdfTextExtractor.getExtension("empty.pdf")).thenReturn("pdf");
        when(pdfTextExtractor.extractText(content)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reader.read(content, "empty.pdf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PDF text extraction returned empty");
    }
}
