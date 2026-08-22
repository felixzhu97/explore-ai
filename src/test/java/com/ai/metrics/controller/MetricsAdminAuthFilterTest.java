package com.ai.metrics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsAdminAuthFilter")
class MetricsAdminAuthFilterTest {

  @Mock private FilterChain filterChain;

  private MetricsAdminProperties properties;
  private MetricsAdminAuthFilter filter;

  @BeforeEach
  void setUp() {
    properties = new MetricsAdminProperties();
    properties.setAdminApiKey("secret-key");
    filter = new MetricsAdminAuthFilter(properties);
  }

  @Test
  void shouldAllowWhenAdminKeyMatches() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/metrics/overview");
    request.addHeader(MetricsAdminAuthFilter.ADMIN_KEY_HEADER, "secret-key");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldRejectWhenAdminKeyMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/metrics/overview");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("METRICS_ADMIN_REQUIRED");
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void shouldNotFilterWhenAdminKeyNotConfigured() throws Exception {
    properties.setAdminApiKey("");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/metrics/overview");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }
}
