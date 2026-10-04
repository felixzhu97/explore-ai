package com.ai.rag.controller.dto;

import com.ai.rag.domain.model.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

/** Upload document response DTO. */
public record UploadDocumentResponse(
    UUID id, String title, DocumentStatus status, int chunkCount, Instant createdAt) {}
