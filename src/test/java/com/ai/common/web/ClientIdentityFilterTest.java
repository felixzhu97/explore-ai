package com.ai.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientIdentityFilter")
class ClientIdentityFilterTest {

  private static final String SERVICE_KEY = "bff-secret";
  private static final String CLIENT_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  private ClientIdentityProperties properties;
  private ServiceAuthProperties serviceAuthProperties;
  private ClientIdentityFilter filter;

  @BeforeEach
  void setUp() {
    properties = new ClientIdentityProperties();
    properties.setCookieName("ea_cid");
    properties.setSecure(false);
    properties.setSameSite("Lax");
    serviceAuthProperties = new ServiceAuthProperties();
    filter =
        new ClientIdentityFilter(
            new ClientIdentityCookieFactory(properties), serviceAuthProperties);
  }

  @Test
  void shouldIssueCookieAndSetAttributeWhenCookieMissing() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/sessions");
    when(request.getCookies()).thenReturn(null);

    filter.doFilter(request, response, filterChain);

    ArgumentCaptor<String> attr = ArgumentCaptor.forClass(String.class);
    verify(request).setAttribute(eq(ClientIdentity.REQUEST_ATTRIBUTE), attr.capture());
    assertThat(attr.getValue())
        .matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    ArgumentCaptor<String> header = ArgumentCaptor.forClass(String.class);
    verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), header.capture());
    assertThat(header.getValue()).contains("ea_cid=");
    assertThat(header.getValue()).contains("HttpOnly");
    assertThat(header.getValue()).contains("SameSite=Lax");
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldReuseExistingCookieWhenValidUuidPresent() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/sessions");
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("ea_cid", CLIENT_ID)});

    filter.doFilter(request, response, filterChain);

    verify(request).setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, CLIENT_ID);
    verify(response, never()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldNotFilterWhenPathOutsideApi() throws Exception {
    when(request.getRequestURI()).thenReturn("/assets/main.js");

    filter.doFilter(request, response, filterChain);

    verify(request, never()).setAttribute(eq(ClientIdentity.REQUEST_ATTRIBUTE), any());
    verify(filterChain).doFilter(request, response);
  }

  @Nested
  @DisplayName("service-key path")
  class ServiceKeyPath {

    @BeforeEach
    void enableServiceAuth() {
      serviceAuthProperties.setApiKey(SERVICE_KEY);
    }

    @Test
    void shouldUseClientIdHeaderWhenServiceKeyValid() throws Exception {
      when(request.getRequestURI()).thenReturn("/api/sessions");
      when(request.getHeader(ClientIdentityFilter.SERVICE_KEY_HEADER)).thenReturn(SERVICE_KEY);
      when(request.getHeader(ClientIdentityFilter.CLIENT_ID_HEADER)).thenReturn(CLIENT_ID);

      filter.doFilter(request, response, filterChain);

      verify(request).setAttribute(ClientIdentity.REQUEST_ATTRIBUTE, CLIENT_ID);
      verify(response, never()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
      verify(request, never()).getCookies();
      verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFallBackToCookieWhenServiceKeyInvalid() throws Exception {
      when(request.getRequestURI()).thenReturn("/api/sessions");
      when(request.getHeader(ClientIdentityFilter.SERVICE_KEY_HEADER)).thenReturn("wrong-key");
      when(request.getCookies()).thenReturn(null);

      filter.doFilter(request, response, filterChain);

      ArgumentCaptor<String> attr = ArgumentCaptor.forClass(String.class);
      verify(request).setAttribute(eq(ClientIdentity.REQUEST_ATTRIBUTE), attr.capture());
      assertThat(attr.getValue()).isNotEqualTo(CLIENT_ID);
      verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
      verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFallBackToCookieWhenClientIdHeaderInvalid() throws Exception {
      when(request.getRequestURI()).thenReturn("/api/sessions");
      when(request.getHeader(ClientIdentityFilter.SERVICE_KEY_HEADER)).thenReturn(SERVICE_KEY);
      when(request.getHeader(ClientIdentityFilter.CLIENT_ID_HEADER)).thenReturn("not-a-uuid");
      when(request.getCookies()).thenReturn(null);

      filter.doFilter(request, response, filterChain);

      verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
      verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFallBackToCookieWhenServiceKeyNotConfigured() throws Exception {
      serviceAuthProperties.setApiKey("");
      when(request.getRequestURI()).thenReturn("/api/sessions");
      when(request.getCookies()).thenReturn(null);

      filter.doFilter(request, response, filterChain);

      verify(response).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
      verify(filterChain).doFilter(request, response);
    }
  }
}
