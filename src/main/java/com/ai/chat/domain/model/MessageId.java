package com.ai.chat.domain.model;

import com.ai.common.domain.model.AbstractEmbeddable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/** Message ID value object ensuring type safety for message identifiers. */
@Getter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(staticName = "createId")
public final class MessageId extends AbstractEmbeddable {

  @NonNull private UUID value;

  /** Parses an id from its UUID text. */
  public static MessageId parseId(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Id cannot be blank");
    }
    return createId(UUID.fromString(text.strip()));
  }

  /** Creates a new random id. */
  public static MessageId generateId() {
    return createId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
