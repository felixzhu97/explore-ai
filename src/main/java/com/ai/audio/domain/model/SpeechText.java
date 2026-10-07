package com.ai.audio.domain.model;

import com.ai.common.exception.DomainException;
import lombok.Value;

/** Text to be spoken, non-blank and capped in length. */
@Value
public class SpeechText {
  String value;

  private static final int MAX_LENGTH = 10_000;

  public SpeechText(String value) {
    if (value == null || value.isBlank()) {
      throw DomainException.createInvalidError(
          "INVALID_SPEECH_TEXT", "Speech text must not be blank");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw DomainException.createInvalidError(
          "INVALID_SPEECH_TEXT", "Speech text exceeds maximum length of " + MAX_LENGTH);
    }
    this.value = value;
  }

  /** Creates validated speech text. */
  public static SpeechText createText(String text) {
    return new SpeechText(text);
  }

  /** Returns the number of whitespace-separated words in the text. */
  public int countWords() {
    if (value.isBlank()) {
      return 0;
    }
    return value.trim().split("\\s+").length;
  }
}
