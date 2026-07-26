package com.ai.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsrfProtectionFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CsrfProtectionFilter filter;
    private ByteArrayOutputStream body;

    @BeforeEach
    void setUp() throws Exception {
        filter = new CsrfProtectionFilter();
        body = new ByteArrayOutputStream();
        lenient().when(response.getOutputStream()).thenReturn(new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
            }

            @Override
            public void write(int b) {
                body.write(b);
            }
        });
    }

    @Test
    void should_allowPost_whenCsrfHeaderPresent() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/sessions");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(CsrfProtectionFilter.HEADER_NAME))
                .thenReturn(CsrfProtectionFilter.HEADER_VALUE);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(403);
    }

    @Test
    void should_rejectPost_whenCsrfHeaderMissing() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/sessions");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(CsrfProtectionFilter.HEADER_NAME)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(403);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(body.toString()).contains("CSRF_REJECTED");
    }

    @Test
    void should_skipGet_whenSafeMethod() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
