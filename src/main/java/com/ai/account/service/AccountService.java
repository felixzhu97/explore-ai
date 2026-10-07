package com.ai.account.service;

import com.ai.account.controller.dto.AccountMeResponse;
import com.ai.account.controller.dto.AccountMode;
import com.ai.account.controller.dto.AccountPlan;
import com.ai.account.controller.dto.LoginProvider;
import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.infra.config.OAuthExploreIamProperties;
import com.ai.account.infra.config.OAuthGithubProperties;
import com.ai.account.infra.config.OAuthGoogleProperties;
import com.ai.billing.service.BillingPlanService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the current account from IAM JWT, OAuth session, or linked Client Identity. */
@Service
@EnableConfigurationProperties({
  OAuthGoogleProperties.class,
  OAuthGithubProperties.class,
  OAuthExploreIamProperties.class
})
@RequiredArgsConstructor
public class AccountService {

  private final AccountUserRepository accountUserRepository;
  private final IamAccountService iamAccountService;
  private final BillingPlanService billingPlanService;
  private final OAuthGoogleProperties oauthGoogleProperties;
  private final OAuthGithubProperties oauthGithubProperties;
  private final OAuthExploreIamProperties oauthExploreIamProperties;

  /** Returns the viewer's account state for the request's Client Identity. */
  @Transactional
  public AccountMeResponse getCurrentAccount(String clientId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return authenticated(clientId, iamAccountService.signIn(jwtAuth.getToken()));
    }
    Optional<OAuthSignIn> signIn = OAuthSignIn.from(authentication);
    if (signIn.isPresent()) {
      return accountUserRepository
          .findByIdentity(signIn.get().identity())
          .map(user -> authenticated(clientId, user))
          .orElseGet(() -> authenticated(clientId, signIn.get()));
    }

    // Session may be missing after a host mismatch; Client Identity link still proves login.
    if (ClientId.isValid(clientId)) {
      Optional<AccountUser> byClient =
          accountUserRepository.findByLinkedClientId(ClientId.parse(clientId));
      if (byClient.isPresent()) {
        return authenticated(clientId, byClient.get());
      }
    }

    return new AccountMeResponse(
        AccountMode.ANONYMOUS,
        clientId,
        null,
        null,
        null,
        AccountPlan.from(billingPlanService.currentPlan()),
        isLoginAvailable(),
        loginProviders());
  }

  /** Links the OAuth sign-in to the browser, unlinking any other account, and returns it. */
  @Transactional
  public AccountUser linkOAuthUser(OAuthSignIn signIn, ClientId clientId) {
    AccountUser user =
        accountUserRepository
            .findByIdentity(signIn.identity())
            .orElseGet(
                () -> AccountUser.create(signIn.identity(), signIn.email(), signIn.displayName()));
    accountUserRepository
        .findByLinkedClientId(clientId)
        .filter(previous -> !previous.getId().equals(user.getId()))
        .ifPresent(
            previous -> {
              previous.unlinkBrowser();
              accountUserRepository.save(previous);
            });
    user.linkBrowser(clientId, signIn.email(), signIn.displayName());
    return accountUserRepository.save(user);
  }

  /** Clears OAuth ↔ Client Identity link so the browser returns to guest mode. */
  @Transactional
  public void unlinkClient(String clientId) {
    if (!ClientId.isValid(clientId)) {
      return;
    }
    accountUserRepository
        .findByLinkedClientId(ClientId.parse(clientId))
        .ifPresent(
            user -> {
              user.unlinkBrowser();
              accountUserRepository.save(user);
            });
  }

  /** Tells whether at least one login provider is available. */
  public boolean isLoginAvailable() {
    return !loginProviders().isEmpty();
  }

  /** Registration ids that are currently configured (e.g. {@code google}, {@code github}). */
  public List<LoginProvider> loginProviders() {
    List<LoginProvider> providers = new ArrayList<>(3);
    if (oauthGoogleProperties.isReady()) {
      providers.add(LoginProvider.GOOGLE);
    }
    if (oauthGithubProperties.isReady()) {
      providers.add(LoginProvider.GITHUB);
    }
    if (oauthExploreIamProperties.isReady()) {
      providers.add(LoginProvider.EXPLORE_IAM);
    }
    return List.copyOf(providers);
  }

  private AccountMeResponse authenticated(String clientId, AccountUser user) {
    return authenticated(
        clientId, user.getId().value(), user.getEmail(), user.displayLabel().orElse(null));
  }

  private AccountMeResponse authenticated(String clientId, OAuthSignIn signIn) {
    return authenticated(
        clientId, signIn.identity().subject(), signIn.email(), signIn.displayLabel().orElse(null));
  }

  private AccountMeResponse authenticated(
      String clientId, String userId, ContactEmail email, String displayName) {
    return new AccountMeResponse(
        AccountMode.AUTHENTICATED,
        clientId,
        userId,
        email == null ? null : email.value(),
        displayName,
        AccountPlan.from(billingPlanService.currentPlan()),
        isLoginAvailable(),
        loginProviders());
  }
}
