package com.ai.scim.infra;

import com.unboundid.scim2.common.messages.ErrorResponse;
import org.springframework.http.ResponseEntity;

/** Builds SCIM error bodies (RFC 7644 §3.12). */
public final class ScimErrors {

  private ScimErrors() {}

  /**
   * Builds an error body.
   *
   * @param status HTTP status
   * @param scimType SCIM error type, may be {@code null}
   * @param detail human-readable detail without personal data
   * @return error body
   */
  public static ErrorResponse error(int status, String scimType, String detail) {
    ErrorResponse error = new ErrorResponse(status);
    error.setScimType(scimType);
    error.setDetail(detail);
    return error;
  }

  /**
   * Maps a service-layer HTTP status to a SCIM error.
   *
   * @param status HTTP status
   * @param reason reason phrase
   * @return error body
   */
  public static ErrorResponse fromStatus(int status, String reason) {
    String scimType = null;
    if (status == 409) {
      scimType = "uniqueness";
    } else if (status == 400) {
      scimType = "invalidValue";
    }
    return error(status, scimType, reason);
  }

  /**
   * Wraps an error body with its status and SCIM media type.
   *
   * @param error error body
   * @return response entity
   */
  public static ResponseEntity<ErrorResponse> toResponse(ErrorResponse error) {
    return ResponseEntity.status(error.getStatus())
        .contentType(ScimJsonHttpMessageConverter.SCIM_JSON)
        .body(error);
  }
}
