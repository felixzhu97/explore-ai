package com.ai.account.controller;

import com.ai.account.controller.dto.AccountMeResponse;
import com.ai.account.service.usecase.AccountUseCase;
import com.ai.common.controller.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account API: guest Client Identity, optional OAuth Login, or IAM Bearer JWT (native).
 *
 * @see <a
 *     href="https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html">JWT
 *     Resource Server</a>
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
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken) {
      return ResponseEntity.ok(accountUseCase.currentAccount(null));
    }
    String clientId = ClientIdentity.require(request);
    return ResponseEntity.ok(accountUseCase.currentAccount(clientId));
  }
}
