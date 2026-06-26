package com.ai.rag.infrastructure.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * PDF text extractor using Apache PDFBox.
 * Extracts text content from PDF files for RAG processing.
 */
@Component
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    public Optional<String> extractText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            log.warn("Cannot extract text from empty PDF bytes");
            return Optional.empty();
        }

        try (InputStream is = new ByteArrayInputStream(bytes);
             PDDocument document = Loader.loadPDF(bytes)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);

            if (text == null || text.isBlank()) {
                log.warn("No text content extracted from PDF");
                return Optional.empty();
            }

            return Optional.of(text.trim());

        } catch (IOException e) {
            log.error("Failed to extract text from PDF", e);
            return Optional.empty();
        }
    }

    public boolean isPdf(byte[] content) {
        if (content == null || content.length < 4) {
            return false;
        }
        return content[0] == 0x25 &&
               content[1] == 0x50 &&
               content[2] == 0x44 &&
               content[3] == 0x46;
    }

    public String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
