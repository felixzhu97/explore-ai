package com.ai.common.controller.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("CsrfProtectionFilter")
class CsrfProtectionFilterTest {

  private static final String API_PATH = "/api/chat/sessions";

  private final CsrfProtectionFilter filter = new CsrfProtectionFilter();
  private final MockHttpServletResponse response = new MockHttpServletResponse();
  private final MockFilterChain chain = new MockFilterChain();

  @Test
  @DisplayName("should pass a post through when the csrf header is present")
  void shouldPassAPostThroughWhenTheCsrfHeaderIsPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", API_PATH);
    request.addHeader(CsrfProtectionFilter.HEADER_NAME, CsrfProtectionFilter.HEADER_VALUE);

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("should reject a post with 403 when the csrf header is missing")
  void shouldRejectAPostWith403WhenTheCsrfHeaderIsMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", API_PATH);

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isNull();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("CSRF_REJECTED");
  }

  @Test
  @DisplayName("should pass a post through when it carries a bearer token")
  void shouldPassAPostThroughWhenItCarriesABearerToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", API_PATH);
    request.addHeader("Authorization", "Bearer iam-access-token");

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isSameAs(request);
  }

  @Test
  @DisplayName("should pass a get through without the csrf header")
  void shouldPassAGetThroughWithoutTheCsrfHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", API_PATH);

    filter.doFilter(request, response, chain);

    assertThat(chain.getRequest()).isSameAs(request);
  }
}
