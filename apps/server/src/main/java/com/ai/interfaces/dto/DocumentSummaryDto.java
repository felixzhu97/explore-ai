package com.ai.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Document summary DTO.
 */
public record DocumentSummaryDto(
    UUID id,
    String title,
    String status,
    Instant createdAt,
    int chunkCount
) {}
