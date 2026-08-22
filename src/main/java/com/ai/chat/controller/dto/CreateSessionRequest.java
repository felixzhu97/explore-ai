package com.ai.chat.controller.dto;

import jakarta.validation.constraints.Size;

/**
 * Create session request DTO.
 *
 * @param title optional session title
 */
public record CreateSessionRequest(
    @Size(max = 100, message = "Title cannot exceed 100 characters") String title) {

  /** Factory for a create-session request with the given title. */
  public static CreateSessionRequest of(String title) {
    return new CreateSessionRequest(title);
  }
}
