package com.ai.common.controller;

import jakarta.servlet.http.HttpServletRequest;

/** Anonymous browser identity resolved from an HttpOnly cookie (OWASP session guidance). */
public final class ClientIdentity {

  public static final String REQUEST_ATTRIBUTE = "com.ai.common.controller.ClientIdentity";

  /** Set to {@code Boolean.TRUE} when the identity came from a trusted BFF service key. */
  public static final String TRUSTED_SERVICE_ATTRIBUTE =
      "com.ai.common.controller.ClientIdentity.trustedService";

  private ClientIdentity() {}

  /** Tells whether the request was authenticated with the trusted BFF service key. */
  public static boolean isTrustedService(HttpServletRequest request) {
    return Boolean.TRUE.equals(request.getAttribute(TRUSTED_SERVICE_ATTRIBUTE));
  }

  /**
   * Returns the client IP used for abuse limits, or {@code null} for trusted BFF requests, which
   * all share the BFF host address.
   */
  public static String resolveLimitAddress(HttpServletRequest request) {
    return isTrustedService(request) ? null : request.getRemoteAddr();
  }

  /** Returns the client id stored on the request, throwing when it is missing or blank. */
  public static String requireClientId(HttpServletRequest request) {
    Object value = request.getAttribute(REQUEST_ATTRIBUTE);
    if (!(value instanceof String clientId) || clientId.isBlank()) {
      throw new ClientIdentityRequiredException();
    }
    return clientId;
  }
}
