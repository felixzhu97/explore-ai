package com.ai.chat.domain.exception;

/** Thrown when a chat session is not found. */
public class ChatSessionNotFoundException extends RuntimeException {

  private final String sessionId;

  public ChatSessionNotFoundException(String sessionId) {
    super("Chat session not found: " + sessionId);
    this.sessionId = sessionId;
  }

  /** Returns the id of the missing session. */
  public String getSessionId() {
    return sessionId;
  }
}
