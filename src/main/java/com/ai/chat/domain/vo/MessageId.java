package com.ai.chat.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Message ID value object ensuring type safety for message identifiers. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class MessageId extends AbstractUuidId {

  /** Documentation. */
  public MessageId(String value) {
    super(value);
  }

  /** Documentation. */
  public static MessageId of(String value) {
    return new MessageId(value);
  }

  /** Documentation. */
  public static MessageId generate() {
    return new MessageId(newUuidString());
  }
}
