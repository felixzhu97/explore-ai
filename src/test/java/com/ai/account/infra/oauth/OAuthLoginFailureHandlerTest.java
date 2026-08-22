package com.ai.account.infra.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OAuthLoginFailureHandler")
class OAuthLoginFailureHandlerTest {

  @Test
  void shouldAppendLoginQueryWhenRedirectHasNoQuery() {
    assertThat(OAuthLoginFailureHandler.withLoginParam("http://127.0.0.1:4200/", "success"))
        .isEqualTo("http://127.0.0.1:4200/?login=success");
  }

  @Test
  void shouldReplaceLoginQueryWhenAlreadyPresent() {
    assertThat(
            OAuthLoginFailureHandler.withLoginParam(
                "http://127.0.0.1:4200/?login=error", "success"))
        .isEqualTo("http://127.0.0.1:4200/?login=success");
  }
}
