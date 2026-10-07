package com.ai.rag.infra.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * PDF text extractor using Apache PDFBox. Extracts text content from PDF files for RAG processing.
 */
@Component
public class PdfTextExtractor {

  /** Extracts position-sorted text from the PDF, or empty if unreadable or textless. */
  public Optional<String> extractText(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return Optional.empty();
    }

    try (InputStream is = new ByteArrayInputStream(bytes);
        PDDocument document = Loader.loadPDF(bytes)) {

      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);

      String text = stripper.getText(document);

      if (text == null || text.isBlank()) {
        return Optional.empty();
      }

      return Optional.of(text.trim());

    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /** Checks whether the bytes start with the {@code %PDF} magic number. */
  public boolean isPdf(byte[] content) {
    if (content == null || content.length < 4) {
      return false;
    }
    return content[0] == 0x25 && content[1] == 0x50 && content[2] == 0x44 && content[3] == 0x46;
  }

  /** Returns the lowercase file extension without the dot, or an empty string if none. */
  public String getExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "";
    }
    return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
  }
}
