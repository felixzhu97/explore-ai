package com.ai.audio.web.dto;

/**
 * Transcription result from ASR service.
 *
 * @param type  Result type: partial or final
 * @param text  Transcribed text
 */
public record TranscriptionResponse(String type, String text) {}
