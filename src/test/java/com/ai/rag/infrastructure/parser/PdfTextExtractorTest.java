package com.ai.rag.infrastructure.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PdfTextExtractor")
class PdfTextExtractorTest {

    private PdfTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfTextExtractor();
    }

    @Nested
    @DisplayName("isPdf()")
    class IsPdf {

        @Test
        @DisplayName("should return true for valid PDF header")
        void shouldReturnTrueForValidPdfHeader() {
            byte[] pdfBytes = "%PDF-1.4".getBytes();

            assertThat(extractor.isPdf(pdfBytes)).isTrue();
        }

        @Test
        @DisplayName("should return false for null content")
        void shouldReturnFalseForNullContent() {
            assertThat(extractor.isPdf(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for content shorter than 4 bytes")
        void shouldReturnFalseForContentShorterThanFourBytes() {
            assertThat(extractor.isPdf(new byte[0])).isFalse();
            assertThat(extractor.isPdf(new byte[3])).isFalse();
        }

        @Test
        @DisplayName("should return false for non-PDF content")
        void shouldReturnFalseForNonPdfContent() {
            byte[] textBytes = "Hello World".getBytes();

            assertThat(extractor.isPdf(textBytes)).isFalse();
        }

        @Test
        @DisplayName("should return false for image bytes")
        void shouldReturnFalseForImageBytes() {
            byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47};

            assertThat(extractor.isPdf(pngHeader)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty byte array")
        void shouldReturnFalseForEmptyByteArray() {
            assertThat(extractor.isPdf(new byte[]{})).isFalse();
        }
    }

    @Nested
    @DisplayName("getExtension()")
    class GetExtension {

        @Test
        @DisplayName("should return lowercase extension for PDF file")
        void shouldReturnLowercaseExtensionForPdfFile() {
            assertThat(extractor.getExtension("document.pdf")).isEqualTo("pdf");
        }

        @Test
        @DisplayName("should return lowercase extension for uppercase filename")
        void shouldReturnLowercaseExtensionForUppercaseFilename() {
            assertThat(extractor.getExtension("DOCUMENT.PDF")).isEqualTo("pdf");
        }

        @Test
        @DisplayName("should return empty string for null filename")
        void shouldReturnEmptyStringForNullFilename() {
            assertThat(extractor.getExtension(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for filename without extension")
        void shouldReturnEmptyStringForFilenameWithoutExtension() {
            assertThat(extractor.getExtension("document")).isEmpty();
        }

        @Test
        @DisplayName("should return gitignore for hidden file starting with dot")
        void shouldReturnGitignoreForHiddenFileStartingWithDot() {
            assertThat(extractor.getExtension(".gitignore")).isEqualTo("gitignore");
        }

        @Test
        @DisplayName("should handle multiple dots in filename")
        void shouldHandleMultipleDotsInFilename() {
            assertThat(extractor.getExtension("my.document.v2.pdf")).isEqualTo("pdf");
        }

        @Test
        @DisplayName("should return correct extension for various file types")
        void shouldReturnCorrectExtensionForVariousFileTypes() {
            assertThat(extractor.getExtension("file.txt")).isEqualTo("txt");
            assertThat(extractor.getExtension("file.docx")).isEqualTo("docx");
            assertThat(extractor.getExtension("file.html")).isEqualTo("html");
        }
    }

    @Nested
    @DisplayName("extractText()")
    class ExtractText {

        @Test
        @DisplayName("should return empty for null bytes")
        void shouldReturnEmptyForNullBytes() {
            assertThat(extractor.extractText(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty bytes")
        void shouldReturnEmptyForEmptyBytes() {
            assertThat(extractor.extractText(new byte[0])).isEmpty();
        }

        @Test
        @DisplayName("should return empty for non-PDF bytes")
        void shouldReturnEmptyForNonPdfBytes() {
            assertThat(extractor.extractText("Hello".getBytes())).isEmpty();
        }

        @Test
        @DisplayName("should return empty for PDF with no extractable text")
        void shouldReturnEmptyForPdfWithNoExtractableText() {
            byte[] minimalPdf = createMinimalPdf();

            Optional<String> result = extractor.extractText(minimalPdf);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should not throw exception on corrupt PDF")
        void shouldNotThrowExceptionOnCorruptPdf() {
            byte[] corruptPdf = "%PDF-1.4\ncorrupt data".getBytes();

            Optional<String> result = extractor.extractText(corruptPdf);

            assertThat(result).isEmpty();
        }
    }

    private byte[] createMinimalPdf() {
        return "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF".getBytes();
    }
}
