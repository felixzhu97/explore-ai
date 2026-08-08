package com.ai.account.web;

import com.ai.account.application.usecase.AccountUseCase;
import com.ai.account.web.dto.AccountMeResponse;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account API: guest Client Identity today; optional Google OAuth when configured.
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html">Spring Security OAuth2 Login</a>
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountUseCase accountUseCase;

    public AccountController(AccountUseCase accountUseCase) {
        this.accountUseCase = accountUseCase;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountMeResponse> me(HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return ResponseEntity.ok(accountUseCase.currentAccount(clientId));
    }
}
