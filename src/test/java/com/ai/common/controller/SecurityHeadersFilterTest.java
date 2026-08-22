package com.ai.common.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityHeadersFilter")
class SecurityHeadersFilterTest {

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  private SecurityHeadersFilter filter;

  @BeforeEach
  void setUp() {
    filter = new SecurityHeadersFilter();
  }

  @Test
  void shouldSetContentSecurityPolicyWhenFiltering() throws Exception {
    when(request.isSecure()).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
    verify(response, org.mockito.Mockito.atLeastOnce()).setHeader(name.capture(), value.capture());
    assertThat(name.getAllValues()).contains("Content-Security-Policy");
    int idx = name.getAllValues().indexOf("Content-Security-Policy");
    assertThat(value.getAllValues().get(idx)).contains("frame-ancestors 'none'");
    verify(filterChain).doFilter(request, response);
  }
}
