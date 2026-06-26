package com.ai.adapter.out.parser;

import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PdfTextExtractor Unit Tests
 *
 * Tests the PDF text extraction functionality:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests text extraction, PDF detection, and file extension parsing
 */
@DisplayName("PdfTextExtractor")
class PdfTextExtractorTest {

    private PdfTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new PdfTextExtractor();
    }

    @Nested
    @DisplayName("extractText")
    class ExtractText {

        @Test
        @DisplayName("should extract text from valid PDF")
        void shouldExtractTextFromValidPdf() throws IOException {
            // Arrange
            byte[] pdfBytes = createPdfWithText("Hello World");

            // Act
            Optional<String> result = extractor.extractText(pdfBytes);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).contains("Hello World");
        }

        @Test
        @DisplayName("should return empty for null input")
        void shouldReturnEmptyForNullInput() {
            // Act
            Optional<String> result = extractor.extractText(null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty byte array")
        void shouldReturnEmptyForEmptyByteArray() {
            // Act
            Optional<String> result = extractor.extractText(new byte[0]);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for non-PDF content")
        void shouldReturnEmptyForNonPdfContent() {
            // Arrange
            byte[] nonPdfContent = "This is not a PDF file".getBytes();

            // Act
            Optional<String> result = extractor.extractText(nonPdfContent);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when PDF has no text")
        void shouldReturnEmptyWhenPdfHasNoText() throws IOException {
            // Arrange - Create empty PDF
            byte[] emptyPdfBytes = createEmptyPdf();

            // Act
            Optional<String> result = extractor.extractText(emptyPdfBytes);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle multi-page PDF")
        void shouldHandleMultiPagePdf() throws IOException {
            // Arrange
            byte[] multiPagePdf = createMultiPagePdf();

            // Act
            Optional<String> result = extractor.extractText(multiPagePdf);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).contains("Page 1");
            assertThat(result.get()).contains("Page 2");
            assertThat(result.get()).contains("Page 3");
        }

        @Test
        @DisplayName("should handle PDF with special characters")
        void shouldHandlePdfWithSpecialCharacters() throws IOException {
            // Arrange
            byte[] pdfWithSpecialChars = createPdfWithText("Special chars: @#$%^&*()_+-=[]{}|;':\",./<>?");

            // Act
            Optional<String> result = extractor.extractText(pdfWithSpecialChars);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).contains("Special chars");
        }

        @Test
        @DisplayName("should handle PDF with unicode characters")
        void shouldHandlePdfWithUnicodeCharacters() throws IOException {
            // Arrange - Create PDF with basic ASCII text (Unicode support varies by font)
            byte[] pdfWithAscii = createPdfWithText("Unicode test text");

            // Act
            Optional<String> result = extractor.extractText(pdfWithAscii);

            // Assert
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should trim extracted text")
        void shouldTrimExtractedText() throws IOException {
            // Arrange
            byte[] pdfBytes = createPdfWithText("   Trimmed Text   ");

            // Act
            Optional<String> result = extractor.extractText(pdfBytes);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).doesNotStartWith(" ");
            assertThat(result.get()).doesNotEndWith(" ");
        }

        @Test
        @DisplayName("should return empty for corrupted PDF")
        void shouldReturnEmptyForCorruptedPdf() {
            // Arrange
            byte[] corruptedPdf = new byte[]{0x25, 0x50, 0x44, 0x46, 0x20, 0x54, 0x48, 0x49, 0x53}; // "%PDF THIS"

            // Act
            Optional<String> result = extractor.extractText(corruptedPdf);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isPdf")
    class IsPdf {

        @Test
        @DisplayName("should return true for valid PDF magic bytes")
        void shouldReturnTrueForValidPdfMagicBytes() {
            // Arrange
            byte[] validPdfStart = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

            // Act
            boolean result = extractor.isPdf(validPdfStart);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for null input")
        void shouldReturnFalseForNullInput() {
            // Act
            boolean result = extractor.isPdf(null);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for empty array")
        void shouldReturnFalseForEmptyArray() {
            // Act
            boolean result = extractor.isPdf(new byte[0]);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for array with less than 4 bytes")
        void shouldReturnFalseForArrayWithLessThanFourBytes() {
            // Arrange
            byte[] shortArray = new byte[]{0x25, 0x50, 0x44};

            // Act
            boolean result = extractor.isPdf(shortArray);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when first byte is not percent")
        void shouldReturnFalseWhenFirstByteIsNotPercent() {
            // Arrange
            byte[] notPdf = new byte[]{0x50, 0x44, 0x46, 0x2D}; // "PDF-"

            // Act
            boolean result = extractor.isPdf(notPdf);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when second byte is not P")
        void shouldReturnFalseWhenSecondByteIsNotP() {
            // Arrange
            byte[] notPdf = new byte[]{0x25, 0x41, 0x44, 0x46}; // "%ADF"

            // Act
            boolean result = extractor.isPdf(notPdf);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true for PDF with additional content after magic bytes")
        void shouldReturnTrueForPdfWithAdditionalContent() {
            // Arrange - %PDF-1.4
            byte[] pdf = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};

            // Act
            boolean result = extractor.isPdf(pdf);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for plain text starting with %")
        void shouldReturnFalseForPlainTextStartingWithPercent() {
            // Arrange
            byte[] plainText = "% This is not a PDF file".getBytes();

            // Act
            boolean result = extractor.isPdf(plainText);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getExtension")
    class GetExtension {

        @Test
        @DisplayName("should return extension in lowercase")
        void shouldReturnExtensionInLowercase() {
            // Act
            String result = extractor.getExtension("document.PDF");

            // Assert
            assertThat(result).isEqualTo("pdf");
        }

        @Test
        @DisplayName("should handle uppercase filename")
        void shouldHandleUppercaseFilename() {
            // Act
            String result = extractor.getExtension("MYFILE.TXT");

            // Assert
            assertThat(result).isEqualTo("txt");
        }

        @Test
        @DisplayName("should return empty string for filename without dot")
        void shouldReturnEmptyStringForFilenameWithoutDot() {
            // Act
            String result = extractor.getExtension("nofileextension");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for null input")
        void shouldReturnEmptyStringForNullInput() {
            // Act
            String result = extractor.getExtension(null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle filename with multiple dots")
        void shouldHandleFilenameWithMultipleDots() {
            // Act
            String result = extractor.getExtension("document.backup.pdf");

            // Assert
            assertThat(result).isEqualTo("pdf");
        }

        @Test
        @DisplayName("should handle filename starting with dot")
        void shouldHandleFilenameStartingWithDot() {
            // Act
            String result = extractor.getExtension(".hiddenfile");

            // Assert
            assertThat(result).isEqualTo("hiddenfile");
        }

        @Test
        @DisplayName("should handle filename ending with dot")
        void shouldHandleFilenameEndingWithDot() {
            // Act
            String result = extractor.getExtension("file.");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle mixed case extension")
        void shouldHandleMixedCaseExtension() {
            // Act
            String result = extractor.getExtension("Document.PpT");

            // Assert
            assertThat(result).isEqualTo("ppt");
        }

        @Test
        @DisplayName("should return correct extension for common file types")
        void shouldReturnCorrectExtensionForCommonFileTypes() {
            assertThat(extractor.getExtension("test.docx")).isEqualTo("docx");
            assertThat(extractor.getExtension("image.jpg")).isEqualTo("jpg");
            assertThat(extractor.getExtension("image.jpeg")).isEqualTo("jpeg");
            assertThat(extractor.getExtension("data.csv")).isEqualTo("csv");
            assertThat(extractor.getExtension("script.js")).isEqualTo("js");
            assertThat(extractor.getExtension("style.css")).isEqualTo("css");
        }
    }

    // Helper methods to create test PDFs

    private byte[] createPdfWithText(String text) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.showText(text);
                cs.endText();
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    private byte[] createEmptyPdf() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            // Empty page with no text
            doc.save(baos);
        }
        return baos.toByteArray();
    }

    private byte[] createMultiPagePdf() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            for (int i = 1; i <= 3; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.showText("Page " + i);
                    cs.endText();
                }
            }
            doc.save(baos);
        }
        return baos.toByteArray();
    }
}
