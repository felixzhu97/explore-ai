package com.ai.account.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.application.usecase.AccountUseCase;
import com.ai.account.web.dto.AccountMeResponse;
import com.ai.common.web.ClientIdentity;
import com.ai.common.web.ClientIdentityRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController")
class AccountControllerTest {

  @Mock private HttpServletRequest request;

  @Mock private AccountUseCase accountUseCase;

  private AccountController controller;

  @BeforeEach
  void setUp() {
    controller = new AccountController(accountUseCase);
  }

  @Test
  void shouldReturnAnonymousAccountWhenClientIdentityPresent() {
    when(request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE)).thenReturn("cid-123");
    when(accountUseCase.currentAccount("cid-123"))
        .thenReturn(
            new AccountMeResponse(
                "anonymous", "cid-123", null, null, "free", false, java.util.List.of()));

    var response = controller.me(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().mode()).isEqualTo("anonymous");
    assertThat(response.getBody().clientId()).isEqualTo("cid-123");
    assertThat(response.getBody().plan()).isEqualTo("free");
    assertThat(response.getBody().loginAvailable()).isFalse();
    assertThat(response.getBody().userId()).isNull();
    verify(accountUseCase).currentAccount("cid-123");
  }

  @Test
  void shouldFailWhenClientIdentityMissing() {
    when(request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE)).thenReturn(null);

    assertThatThrownBy(() -> controller.me(request))
        .isInstanceOf(ClientIdentityRequiredException.class);
  }
}
