package com.ai.account.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OAuthSpaRedirects")
class OAuthSpaRedirectsTest {

    @Test
    void should_keepCallbackHost_whenConfiguredRedirectIsLoopback() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(4200);

        String target = OAuthSpaRedirects.afterLogin(request, "http://127.0.0.1:4200/", "success");

        assertThat(target).isEqualTo("http://localhost:4200/?login=success");
    }

    @Test
    void should_useConfiguredAbsolute_whenHostIsNotLoopback() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("api.example.com");
        when(request.getServerPort()).thenReturn(443);

        String target = OAuthSpaRedirects.afterLogin(request, "https://www.felixzhu.chat/", "success");

        assertThat(target).isEqualTo("https://www.felixzhu.chat/?login=success");
    }

    @Test
    void should_appendLoginQuery_whenRelativePath() {
        assertThat(OAuthSpaRedirects.withLoginParam("/", "error")).isEqualTo("/?login=error");
    }
}
