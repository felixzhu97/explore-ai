package com.ai.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.controller.OwnerContext;
import com.ai.account.service.usecase.OwnerEraseUseCase;
import com.ai.common.controller.ClientIdentityCookieFactory;
import com.ai.common.controller.ClientIdentityProperties;
import com.ai.common.domain.vo.OwnerKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class PrivacyControllerTest {

  private static final OwnerKey OWNER = OwnerKey.forClient("11111111-1111-1111-1111-111111111111");

  @Mock private OwnerContext ownerContext;

  @Mock private OwnerEraseUseCase ownerEraseUseCase;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  private PrivacyController controller;

  @BeforeEach
  void setUp() {
    ClientIdentityProperties properties = new ClientIdentityProperties();
    properties.setCookieName("ea_cid");
    properties.setSecure(false);
    properties.setSameSite("Lax");
    controller =
        new PrivacyController(
            ownerContext, ownerEraseUseCase, new ClientIdentityCookieFactory(properties));
  }

  @Test
  void shouldEraseAllOwnerDataWhenDeletePrivacySessions() {
    when(ownerContext.require(request)).thenReturn(OWNER);

    var result = controller.eraseAllSessions(request);

    assertThat(result.getStatusCode().value()).isEqualTo(204);
    verify(ownerEraseUseCase).eraseAllForOwner(OWNER);
  }

  @Test
  void shouldRotateCookieWhenResetIdentity() {
    var result = controller.resetIdentity(response);

    assertThat(result.getStatusCode().value()).isEqualTo(204);
    verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
  }
}
