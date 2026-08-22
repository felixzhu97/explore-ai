package com.ai.account.controller;

import com.ai.account.controller.dto.AccountMeResponse;
import com.ai.account.service.usecase.AccountUseCase;
import com.ai.common.controller.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account API: guest Client Identity today; optional Google OAuth when configured.
 *
 * @see <a
 *     href="https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html">Spring
 *     Security OAuth2 Login</a>
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final AccountUseCase accountUseCase;

  /** Documentation. */
  public AccountController(AccountUseCase accountUseCase) {
    this.accountUseCase = accountUseCase;
  }

  /** Documentation. */
  @GetMapping("/me")
  public ResponseEntity<AccountMeResponse> me(HttpServletRequest request) {
    String clientId = ClientIdentity.require(request);
    return ResponseEntity.ok(accountUseCase.currentAccount(clientId));
  }
}
