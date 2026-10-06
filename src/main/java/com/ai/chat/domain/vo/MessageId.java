package com.ai.chat.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Message ID value object ensuring type safety for message identifiers. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class MessageId extends AbstractUuidId {

  public MessageId(String value) {
    super(value);
  }

  /** Wraps an existing message id. */
  public static MessageId of(String value) {
    return new MessageId(value);
  }

  /** Creates a new random message id. */
  public static MessageId generate() {
    return new MessageId(generateUuidString());
  }
}
