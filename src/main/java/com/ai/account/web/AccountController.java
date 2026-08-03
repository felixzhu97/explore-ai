package com.ai.account.web;

import com.ai.account.web.dto.AccountMeResponse;
import com.ai.billing.infrastructure.config.BillingProperties;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account foundation for commercialization (anonymous Client Identity today; OAuth later).
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html">Spring Security OAuth2 Login</a>
 */
@RestController
@RequestMapping("/api/account")
@EnableConfigurationProperties(BillingProperties.class)
public class AccountController {

    private final BillingProperties billingProperties;

    public AccountController(BillingProperties billingProperties) {
        this.billingProperties = billingProperties;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountMeResponse> me(HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return ResponseEntity.ok(new AccountMeResponse(
                "anonymous",
                clientId,
                null,
                null,
                billingProperties.getPlan()
        ));
    }
}
