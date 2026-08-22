package com.ai.common.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsrfProtectionFilter")
class CsrfProtectionFilterTest {

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  private CsrfProtectionFilter filter;
  private ByteArrayOutputStream body;

  @BeforeEach
  void setUp() throws Exception {
    filter = new CsrfProtectionFilter();
    body = new ByteArrayOutputStream();
    lenient()
        .when(response.getOutputStream())
        .thenReturn(
            new jakarta.servlet.ServletOutputStream() {
              @Override
              public boolean isReady() {
                return true;
              }

              @Override
              public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}

              @Override
              public void write(int b) {
                body.write(b);
              }
            });
  }

  @Test
  void shouldAllowPostWhenCsrfHeaderPresent() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/sessions");
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader(CsrfProtectionFilter.HEADER_NAME))
        .thenReturn(CsrfProtectionFilter.HEADER_VALUE);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(response, never()).setStatus(403);
  }

  @Test
  void shouldRejectPostWhenCsrfHeaderMissing() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/sessions");
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader(CsrfProtectionFilter.HEADER_NAME)).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    verify(response).setStatus(403);
    verify(filterChain, never()).doFilter(request, response);
    assertThat(body.toString()).contains("CSRF_REJECTED");
  }

  @Test
  void shouldSkipGetWhenSafeMethod() throws Exception {
    when(request.getMethod()).thenReturn("GET");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }
}
