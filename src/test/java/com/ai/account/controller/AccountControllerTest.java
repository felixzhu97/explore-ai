package com.ai.account.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.controller.dto.AccountMeResponse;
import com.ai.account.service.usecase.AccountUseCase;
import com.ai.common.controller.GlobalExceptionHandler;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = AccountController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AccountController")
class AccountControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private AccountUseCase accountUseCase;

  @Nested
  @DisplayName("GET /api/account/me")
  class Me {

    @Test
    @DisplayName("should return anonymous account when client identity present")
    void shouldReturnAnonymousAccountWhenClientIdentityPresent() {
      when(accountUseCase.currentAccount("cid-123"))
          .thenReturn(
              new AccountMeResponse(
                  "anonymous", "cid-123", null, null, "free", false, java.util.List.of()));

      var result =
          mvc.get()
              .uri("/api/account/me")
              .with(ClientIdentityRequestPostProcessor.withClientId("cid-123"))
              .exchange();

      assertThat(result).hasStatusOk();
      assertThat(result).bodyJson().extractingPath("$.mode").asString().isEqualTo("anonymous");
      assertThat(result).bodyJson().extractingPath("$.clientId").asString().isEqualTo("cid-123");
      assertThat(result).bodyJson().extractingPath("$.plan").asString().isEqualTo("free");
      assertThat(result).bodyJson().extractingPath("$.loginAvailable").asBoolean().isFalse();
      assertThat(result).bodyJson().extractingPath("$.userId").isNull();
      verify(accountUseCase).currentAccount("cid-123");
    }

    @Test
    @DisplayName("should fail when client identity missing")
    void shouldFailWhenClientIdentityMissing() {
      assertThat(mvc.get().uri("/api/account/me"))
          .hasStatus(HttpStatus.UNAUTHORIZED)
          .bodyJson()
          .extractingPath("$.errorCode")
          .asString()
          .isEqualTo("CLIENT_IDENTITY_REQUIRED");
    }
  }
}
