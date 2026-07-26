package com.ai.chat.web;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.common.web.ClientIdentity;
import com.ai.common.web.ClientIdentityCookieFactory;
import com.ai.common.web.ClientIdentityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyControllerTest {

    private static final String CLIENT_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private ChatUseCase chatUseCase;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private PrivacyController controller;

    @BeforeEach
    void setUp() {
        ClientIdentityProperties properties = new ClientIdentityProperties();
        properties.setCookieName("ea_cid");
        properties.setSecure(false);
        properties.setSameSite("Lax");
        controller = new PrivacyController(chatUseCase, new ClientIdentityCookieFactory(properties));
        lenient().when(request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE)).thenReturn(CLIENT_ID);
    }

    @Test
    void should_eraseAllSessions_whenDeletePrivacySessions() {
        doNothing().when(chatUseCase).deleteAllSessionsForClient(CLIENT_ID);

        var result = controller.eraseAllSessions(request);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(chatUseCase).deleteAllSessionsForClient(CLIENT_ID);
    }

    @Test
    void should_rotateCookie_whenResetIdentity() {
        var result = controller.resetIdentity(response);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }
}
