package com.ai.account.infra.oauth;

import com.ai.account.infra.config.OAuthSpaProperties;
import com.ai.account.service.usecase.AccountUseCase;
import com.ai.account.service.usecase.OwnerMergeUseCase;
import com.ai.common.controller.ClientIdentity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Documentation. */
@Component
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);

  private final AccountUseCase accountUseCase;
  private final OwnerMergeUseCase ownerMergeUseCase;
  private final OAuthSpaProperties spaProperties;
  private final SecurityContextRepository securityContextRepository;

  /** Documentation. */
  public OAuthLoginSuccessHandler(
      AccountUseCase accountUseCase,
      OwnerMergeUseCase ownerMergeUseCase,
      OAuthSpaProperties spaProperties,
      SecurityContextRepository securityContextRepository) {
    this.accountUseCase = accountUseCase;
    this.ownerMergeUseCase = ownerMergeUseCase;
    this.spaProperties = spaProperties;
    this.securityContextRepository = securityContextRepository;
    setAlwaysUseDefaultTargetUrl(true);
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    String clientId = resolveClientId(request);
    String provider = registrationId(authentication);
    Object principal = authentication.getPrincipal();
    if (clientId != null) {
      String accountUserId = null;
      if (principal instanceof OidcUser oidcUser) {
        accountUserId =
            accountUseCase.linkOAuthUser(
                provider, oidcUser.getSubject(), oidcUser.getEmail(), clientId);
      } else if (principal instanceof OAuth2User oauth2User) {
        accountUserId =
            accountUseCase.linkOAuthUser(
                provider, oauth2User.getName(), resolveEmail(oauth2User), clientId);
      }
      if (accountUserId != null) {
        ownerMergeUseCase.mergeClientIntoAccount(clientId, accountUserId);
      }
    } else {
      log.warn("OAuth success without Client Identity cookie; session auth only");
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);

    setDefaultTargetUrl(
        OAuthSpaRedirects.afterLogin(request, spaProperties.getSuccessRedirectUrl(), "success"));
    super.onAuthenticationSuccess(request, response, authentication);
  }

  private static String registrationId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken token) {
      return token.getAuthorizedClientRegistrationId();
    }
    return "unknown";
  }

  /** Prefer email; GitHub may only expose {@code login} when the address is private. */
  private static String resolveEmail(OAuth2User oauth2User) {
    String email = oauth2User.getAttribute("email");
    if (email != null && !email.isBlank()) {
      return email.trim();
    }
    String login = oauth2User.getAttribute("login");
    if (login != null && !login.isBlank()) {
      return login.trim();
    }
    String name = oauth2User.getAttribute("name");
    if (name != null && !name.isBlank()) {
      return name.trim();
    }
    return null;
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
