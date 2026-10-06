package com.ai.chat.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for ChatSession. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class ChatSessionId extends AbstractUuidId {

  public ChatSessionId(String value) {
    super(value);
  }

  /** Wraps an existing session id. */
  public static ChatSessionId of(String value) {
    return new ChatSessionId(value);
  }

  /** Creates a new random session id. */
  public static ChatSessionId generate() {
    return new ChatSessionId(generateUuidString());
  }
}
