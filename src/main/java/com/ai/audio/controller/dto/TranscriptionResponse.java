package com.ai.audio.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * WebSocket frame sent to the transcription client.
 *
 * @param text transcript so far for {@code partial}/{@code final}, or the failure message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TranscriptionResponse(TranscriptionType type, String text) {

  /** Normalizes a missing text to empty. */
  public TranscriptionResponse {
    text = text == null ? "" : text;
  }

  /** Creates an error frame with the message. */
  public static TranscriptionResponse createErrorResponse(String text) {
    return new TranscriptionResponse(TranscriptionType.ERROR, text);
  }

  /** Kind of transcription frame. */
  public enum TranscriptionType {
    PARTIAL("partial"),
    FINAL("final"),
    ERROR("error");

    private final String value;

    TranscriptionType(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    /** Parses a wire value case-insensitively; rejects unknown frame kinds. */
    @JsonCreator
    public static TranscriptionType parseType(String raw) {
      String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
      for (TranscriptionType type : values()) {
        if (type.value.equals(normalized)) {
          return type;
        }
      }
      throw new IllegalArgumentException("Unknown transcription type: " + raw);
    }
  }
}
