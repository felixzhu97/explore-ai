package com.ai.common.controller.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.ai.common.controller.ClientIdentity;
import com.ai.common.infra.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientRateLimitFilter")
class ClientRateLimitFilterTest {

  private static final String ADDRESS = "203.0.113.7";

  @Mock private FilterChain filterChain;

  private RateLimitProperties properties;
  private ClientRateLimitFilter filter;

  @BeforeEach
  void setUp() {
    properties = new RateLimitProperties();
    properties.setRequestsPerWindow(2);
    properties.setIpRequestsPerWindow(3);
    properties.setWindowSeconds(30);
    filter =
        new ClientRateLimitFilter(
            properties, Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @DisplayName("should reject with retry after window when client exceeds its limit")
  void shouldRejectWithRetryAfterWindowWhenClientExceedsItsLimit() throws Exception {
    filter.doFilter(
        post("/api/chat/stream", "client-a"), new MockHttpServletResponse(), filterChain);
    filter.doFilter(
        post("/api/chat/stream", "client-a"), new MockHttpServletResponse(), filterChain);
    MockHttpServletResponse third = new MockHttpServletResponse();

    filter.doFilter(post("/api/chat/stream", "client-a"), third, filterChain);

    assertThat(third.getStatus()).isEqualTo(429);
    assertThat(third.getHeader("Retry-After")).isEqualTo("30");
    verify(filterChain, times(2)).doFilter(any(), any());
  }

  @Test
  @DisplayName("should reject fresh client ids when the same ip exceeds its limit")
  void shouldRejectFreshClientIdsWhenTheSameIpExceedsItsLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      filter.doFilter(
          post("/api/workflows/parallel", UUID.randomUUID().toString()),
          new MockHttpServletResponse(),
          filterChain);
    }
    MockHttpServletResponse fourth = new MockHttpServletResponse();

    filter.doFilter(
        post("/api/workflows/parallel", UUID.randomUUID().toString()), fourth, filterChain);

    assertThat(fourth.getStatus()).isEqualTo(429);
    verify(filterChain, times(3)).doFilter(any(), any());
  }

  @Test
  @DisplayName("should skip ip limit when request comes from trusted service")
  void shouldSkipIpLimitWhenRequestComesFromTrustedService() throws Exception {
    for (int i = 0; i < 4; i++) {
      MockHttpServletRequest request = post("/api/rag/chat", UUID.randomUUID().toString());
      request.setAttribute(ClientIdentity.TRUSTED_SERVICE_ATTRIBUTE, Boolean.TRUE);
      filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    }

    verify(filterChain, times(4)).doFilter(any(), any());
  }

  @Test
  @DisplayName("should not limit read requests")
  void shouldNotLimitReadRequests() throws Exception {
    properties.setRequestsPerWindow(0);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chat/sessions");
    request.setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, "client-a");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
    verify(filterChain).doFilter(request, response);
  }

  private static MockHttpServletRequest post(String path, String clientId) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    request.setRemoteAddr(ADDRESS);
    request.setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, clientId);
    return request;
  }
}
