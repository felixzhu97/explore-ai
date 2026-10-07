package com.ai.account.infra.oauth;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ClientId;
import com.ai.account.infra.config.OAuthSpaProperties;
import com.ai.account.service.AccountService;
import com.ai.account.service.OAuthSignIn;
import com.ai.account.service.OwnerMergeService;
import com.ai.common.controller.ClientIdentity;
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

/** Links the OAuth account to an account, merges the client's data, and redirects to the SPA. */
@Component
@ConditionalOnBean(ClientRegistrationRepository.class)
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final AccountService accountService;
  private final OwnerMergeService ownerMergeService;
  private final OAuthSpaProperties spaProperties;
  private final SecurityContextRepository securityContextRepository;

  public OAuthLoginSuccessHandler(
      AccountService accountService,
      OwnerMergeService ownerMergeService,
      OAuthSpaProperties spaProperties,
      SecurityContextRepository securityContextRepository) {
    this.accountService = accountService;
    this.ownerMergeService = ownerMergeService;
    this.spaProperties = spaProperties;
    this.securityContextRepository = securityContextRepository;
    setAlwaysUseDefaultTargetUrl(true);
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    Object attribute = request.getAttribute(ClientIdentity.REQUEST_ATTRIBUTE);
    Optional<OAuthSignIn> signIn = OAuthSignIn.from(authentication);
    if (attribute instanceof String raw && ClientId.isValid(raw) && signIn.isPresent()) {
      Account account = accountService.linkOAuthUser(signIn.get(), ClientId.parseId(raw));
      ownerMergeService.mergeGuestIntoAccount(account);
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
