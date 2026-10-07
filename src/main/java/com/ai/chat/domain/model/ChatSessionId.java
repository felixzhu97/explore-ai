package com.ai.chat.domain.model;

import com.ai.common.domain.model.AbstractEmbeddable;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/** Strongly-typed ID for ChatSession. */
@Embeddable
@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(staticName = "of")
public final class ChatSessionId extends AbstractEmbeddable {

  @NonNull private UUID value;

  /** Parses an id from its UUID text. */
  public static ChatSessionId of(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Id cannot be blank");
    }
    return of(UUID.fromString(text.strip()));
  }

  /** Creates a new random id. */
  public static ChatSessionId generate() {
    return of(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
