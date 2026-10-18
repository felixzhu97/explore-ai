package com.ai.account.infra.oauth;

import com.ai.account.domain.model.ClientId;
import com.ai.account.infra.config.OAuthSpaProperties;
import com.ai.account.service.AccountService;
import com.ai.account.service.OAuthSignIn;
import com.ai.common.controller.ClientIdentity;
import com.ai.common.exception.DomainException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Links the OAuth account to the browser, which merges the guest data, and redirects to the SPA.
 */
@Component
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final AccountService accountService;
  private final OAuthSpaProperties spaProperties;
  private final SecurityContextRepository securityContextRepository;

  public OAuthLoginSuccessHandler(
      AccountService accountService,
      OAuthSpaProperties spaProperties,
      SecurityContextRepository securityContextRepository) {
    this.accountService = accountService;
    this.spaProperties = spaProperties;
    this.securityContextRepository = securityContextRepository;
    setAlwaysUseDefaultTargetUrl(true);
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    Object attribute = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
    Optional<OAuthSignIn> signIn = OAuthSignIn.createSignIn(authentication);
    if (attribute instanceof String raw && ClientId.isValid(raw) && signIn.isPresent()) {
      try {
        accountService.linkOAuthUser(signIn.get(), ClientId.parseId(raw));
      } catch (DomainException e) {
        if (e.getKind() != DomainException.Kind.FORBIDDEN) {
          throw e;
        }
        SecurityContext empty = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(empty);
        securityContextRepository.saveContext(empty, request, response);
        response.sendRedirect(
            OAuthSpaRedirects.buildAfterLoginUrl(
                request, spaProperties.getSuccessRedirectUrl(), "error"));
        return;
      }
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);

    setDefaultTargetUrl(
        OAuthSpaRedirects.buildAfterLoginUrl(
            request, spaProperties.getSuccessRedirectUrl(), "success"));
    super.onAuthenticationSuccess(request, response, authentication);
  }
}
