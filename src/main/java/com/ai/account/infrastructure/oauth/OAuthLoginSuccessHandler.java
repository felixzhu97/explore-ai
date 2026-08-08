package com.ai.account.infrastructure.oauth;

import com.ai.account.application.usecase.AccountUseCase;
import com.ai.account.infrastructure.config.OAuthGoogleProperties;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.oauth.google", name = "enabled", havingValue = "true")
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);

    private final AccountUseCase accountUseCase;
    private final OAuthGoogleProperties properties;
    private final SecurityContextRepository securityContextRepository;

    public OAuthLoginSuccessHandler(
            AccountUseCase accountUseCase,
            OAuthGoogleProperties properties,
            SecurityContextRepository securityContextRepository) {
        this.accountUseCase = accountUseCase;
        this.properties = properties;
        this.securityContextRepository = securityContextRepository;
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String clientId = resolveClientId(request);
        Object principal = authentication.getPrincipal();
        if (clientId != null) {
            if (principal instanceof OidcUser oidcUser) {
                accountUseCase.linkOAuthUser(
                        "google", oidcUser.getSubject(), oidcUser.getEmail(), clientId);
            } else if (principal instanceof OAuth2User oauth2User) {
                String email = oauth2User.getAttribute("email");
                accountUseCase.linkOAuthUser("google", oauth2User.getName(), email, clientId);
            }
        } else {
            log.warn("OAuth success without Client Identity cookie; session auth only");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        setDefaultTargetUrl(OAuthSpaRedirects.afterLogin(request, properties.getSuccessRedirectUrl(), "success"));
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private static String resolveClientId(HttpServletRequest request) {
        Object attribute = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
        if (attribute instanceof String id && !id.isBlank()) {
            return id;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("ea_cid".equals(cookie.getName()) || "__Host-ea_cid".equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }
}
