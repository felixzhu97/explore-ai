package com.ai.rag.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Upload document response DTO.
 */
public record UploadDocumentResponse(
    UUID id,
    String title,
    String status,
    int chunkCount,
    Instant createdAt
) {}
