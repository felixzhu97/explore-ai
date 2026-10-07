package com.ai.chat.domain.model;

import com.ai.common.domain.model.AbstractEmbeddable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Title shown for a chat session; one place for the default text and length limits. */
@Embeddable
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class SessionTitle extends AbstractEmbeddable {

  /** Longest title a user can give a session. */
  public static final int MAX_LENGTH = 100;

  /** Longest title derived from the first exchange, so it fits the session list. */
  public static final int MAX_DERIVED_LENGTH = 50;

  public static final SessionTitle DEFAULT = new SessionTitle("New Chat");

  @NotBlank
  @Size(max = MAX_LENGTH)
  @Column(nullable = false, length = MAX_LENGTH)
  private String title;

  private SessionTitle(String title) {
    this.title = title;
  }

  /** Returns the title a user typed, trimmed and cut to {@value #MAX_LENGTH} characters. */
  public static SessionTitle of(String text) {
    return clean(text, MAX_LENGTH);
  }

  /** Returns a title from the user's first message when no generated title is available. */
  public static SessionTitle fromFirstMessage(String userMessage) {
    return clean(userMessage, MAX_DERIVED_LENGTH);
  }

  /** Returns a title the model wrote for the first exchange, without wrapping quotes. */
  public static SessionTitle generated(String modelTitle) {
    return clean(
        modelTitle == null ? null : modelTitle.strip().replaceAll("^[\"']+|[\"']+$", ""),
        MAX_DERIVED_LENGTH);
  }

  /** Tells whether this is the placeholder title of a session nobody has named yet. */
  public boolean isDefault() {
    return equals(DEFAULT);
  }

  /** Returns the title text. */
  public String value() {
    return title;
  }

  @Override
  public String toString() {
    return title;
  }

  private static SessionTitle clean(String text, int maxLength) {
    if (text == null || text.isBlank()) {
      return DEFAULT;
    }
    String cleaned = text.replaceAll("\\s+", " ").trim();
    if (cleaned.length() > maxLength) {
      cleaned = cleaned.substring(0, maxLength).trim();
    }
    return new SessionTitle(cleaned);
  }
}
