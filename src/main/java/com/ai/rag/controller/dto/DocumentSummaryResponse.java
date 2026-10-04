package com.ai.rag.controller.dto;

import com.ai.rag.domain.model.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

/** Document summary DTO. */
public record DocumentSummaryResponse(
    UUID id, String title, DocumentStatus status, Instant createdAt, int chunkCount) {}
