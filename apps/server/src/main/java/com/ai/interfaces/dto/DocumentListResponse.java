package com.ai.interfaces.dto;

import java.util.List;

/**
 * Document list response DTO.
 */
public record DocumentListResponse(
    List<DocumentSummaryDto> documents
) {}
