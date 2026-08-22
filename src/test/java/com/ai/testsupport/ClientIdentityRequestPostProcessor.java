package com.ai.testsupport;

import com.ai.common.controller.ClientIdentity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Attaches a resolved client id to MockMvc requests (simulates {@link ClientIdentity} filter). */
public final class ClientIdentityRequestPostProcessor {

  private ClientIdentityRequestPostProcessor() {}

  /** Returns a post-processor that sets the client id request attribute. */
  public static RequestPostProcessor withClientId(String clientId) {
    return request -> {
      if (request instanceof MockHttpServletRequest mock) {
        mock.setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, clientId);
      }
      return request;
    };
  }
}
