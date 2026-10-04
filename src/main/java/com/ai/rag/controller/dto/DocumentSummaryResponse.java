package com.ai.rag.controller.dto;

import java.time.Instant;
import java.util.UUID;

/** Document summary DTO. */
public record DocumentSummaryResponse(
    UUID id, String title, String status, Instant createdAt, int chunkCount) {}
