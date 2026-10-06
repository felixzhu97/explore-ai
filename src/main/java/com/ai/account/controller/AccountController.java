package com.ai.account.controller;

import com.ai.account.controller.dto.AccountMeResponse;
import com.ai.account.service.AccountService;
import com.ai.common.controller.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  /** Returns the signed-in or guest account for the current request. */
  @GetMapping("/me")
  public ResponseEntity<AccountMeResponse> getCurrentAccount(HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken) {
      return ResponseEntity.ok(accountService.getCurrentAccount(null));
    }
    String clientId = ClientIdentity.require(request);
    return ResponseEntity.ok(accountService.getCurrentAccount(clientId));
  }
}
