package com.ai.rag.infra.etl;

import com.ai.common.exception.DomainException;
import com.ai.rag.domain.model.ExtractedDocument;
import com.ai.rag.domain.repository.DocumentReader;
import com.ai.rag.infra.parser.PdfTextExtractor;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Infrastructure adapter for reading PDF and plain text documents. */
@Component
@RequiredArgsConstructor
public class PdfAndTextDocumentReader implements DocumentReader {

  private final PdfTextExtractor pdfTextExtractor;

  @Override
  public ExtractedDocument readDocument(byte[] content, String fileName) {
    if (pdfTextExtractor.getExtension(fileName).equalsIgnoreCase("pdf")) {
      String text =
          pdfTextExtractor
              .extractText(content)
              .orElseThrow(
                  () ->
                      DomainException.unprocessable(
                          "DOCUMENT_UNREADABLE", "Could not extract text from " + fileName));
      return new ExtractedDocument(text, Map.of("fileName", fileName), fileName);
    }
    return new ExtractedDocument(new String(content), Map.of("fileName", fileName), fileName);
  }
}
