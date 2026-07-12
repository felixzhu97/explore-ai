package com.ai.rag.infrastructure.etl;

import com.ai.rag.domain.model.RawDocument;
import com.ai.rag.domain.repository.DocumentReader;
import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Infrastructure adapter for reading PDF and plain text documents.
 */
@Component
public class PdfAndTextDocumentReader implements DocumentReader {

    private final PdfTextExtractor pdfTextExtractor;

    public PdfAndTextDocumentReader(PdfTextExtractor pdfTextExtractor) {
        this.pdfTextExtractor = pdfTextExtractor;
    }

    @Override
    public RawDocument read(byte[] content, String fileName) {
        if (pdfTextExtractor.getExtension(fileName).equalsIgnoreCase("pdf")) {
            String text = pdfTextExtractor.extractText(content)
                    .orElseThrow(() -> new IllegalStateException("PDF text extraction returned empty"));
            return new RawDocument(text, Map.of("fileName", fileName), fileName);
        }
        return new RawDocument(new String(content), Map.of("fileName", fileName), fileName);
    }
}
