package com.ai.rag.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Upload document request DTO.
 */
public record UploadDocumentRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Content is required")
    String content,

    String fileName
) {}
