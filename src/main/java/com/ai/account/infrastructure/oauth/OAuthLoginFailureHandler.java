package com.ai.account.infrastructure.oauth;

import com.ai.account.infrastructure.config.OAuthGoogleProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Sends the browser back to the SPA with {@code login=error} after a failed Google OAuth attempt.
 */
@Component
@ConditionalOnProperty(prefix = "app.oauth.google", name = "enabled", havingValue = "true")
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final OAuthGoogleProperties properties;

    public OAuthLoginFailureHandler(OAuthGoogleProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.sendRedirect(
                OAuthSpaRedirects.afterLogin(request, properties.getSuccessRedirectUrl(), "error"));
    }

    /** Visible for unit tests. */
    static String withLoginParam(String redirectUrl, String loginValue) {
        return OAuthSpaRedirects.withLoginParam(redirectUrl, loginValue);
    }
}
