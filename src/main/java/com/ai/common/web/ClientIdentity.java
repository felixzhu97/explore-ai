package com.ai.common.web;

import jakarta.servlet.http.HttpServletRequest;

/** Anonymous browser identity resolved from an HttpOnly cookie (OWASP session guidance). */
public final class ClientIdentity {

  public static final String REQUEST_ATTRIBUTE = "com.ai.common.web.ClientIdentity";

  private ClientIdentity() {}

  /** Documentation. */
  public static String require(HttpServletRequest request) {
    Object value = request.getAttribute(REQUEST_ATTRIBUTE);
    if (!(value instanceof String clientId) || clientId.isBlank()) {
      throw new ClientIdentityRequiredException();
    }
    return clientId;
  }
}
