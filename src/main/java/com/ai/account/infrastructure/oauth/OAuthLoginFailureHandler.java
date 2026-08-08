package com.ai.account.infrastructure.oauth;

import com.ai.account.infrastructure.config.OAuthSpaProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Sends the browser back to the SPA with {@code login=error} after a failed OAuth attempt.
 */
@Component
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthSpaProperties spaProperties;

    public OAuthLoginFailureHandler(OAuthSpaProperties spaProperties) {
        this.spaProperties = spaProperties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.sendRedirect(
                OAuthSpaRedirects.afterLogin(request, spaProperties.getSuccessRedirectUrl(), "error"));
    }

    /** Visible for unit tests. */
    static String withLoginParam(String redirectUrl, String loginValue) {
        return OAuthSpaRedirects.withLoginParam(redirectUrl, loginValue);
    }
}
