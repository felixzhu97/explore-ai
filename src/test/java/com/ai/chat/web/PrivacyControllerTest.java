package com.ai.chat.web;

import com.ai.account.application.usecase.OwnerEraseUseCase;
import com.ai.account.web.OwnerContext;
import com.ai.common.domain.vo.OwnerKey;
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

    private static final OwnerKey OWNER = OwnerKey.forClient("11111111-1111-1111-1111-111111111111");

    @Mock
    private OwnerContext ownerContext;

    @Mock
    private OwnerEraseUseCase ownerEraseUseCase;

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
        controller = new PrivacyController(
                ownerContext, ownerEraseUseCase, new ClientIdentityCookieFactory(properties));
    }

    @Test
    void should_eraseAllOwnerData_whenDeletePrivacySessions() {
        when(ownerContext.require(request)).thenReturn(OWNER);

        var result = controller.eraseAllSessions(request);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(ownerEraseUseCase).eraseAllForOwner(OWNER);
    }

    @Test
    void should_rotateCookie_whenResetIdentity() {
        var result = controller.resetIdentity(response);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
    }
}
