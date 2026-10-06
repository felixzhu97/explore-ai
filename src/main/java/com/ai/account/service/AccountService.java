package com.ai.account.service;

import com.ai.account.controller.dto.AccountMeResponse;
import com.ai.account.controller.dto.AccountMode;
import com.ai.account.controller.dto.AccountPlan;
import com.ai.account.controller.dto.LoginProvider;
import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.infra.config.OAuthExploreIamProperties;
import com.ai.account.infra.config.OAuthGithubProperties;
import com.ai.account.infra.config.OAuthGoogleProperties;
import com.ai.billing.infra.config.BillingProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the current account from IAM JWT, OAuth session, or linked Client Identity. */
@Service
@EnableConfigurationProperties({
  BillingProperties.class,
  OAuthGoogleProperties.class,
  OAuthGithubProperties.class,
  OAuthExploreIamProperties.class
})
@RequiredArgsConstructor
public class AccountService {

  private final AccountUserRepository accountUserRepository;
  private final BillingProperties billingProperties;
  private final OAuthGoogleProperties oauthGoogleProperties;
  private final OAuthGithubProperties oauthGithubProperties;
  private final OAuthExploreIamProperties oauthExploreIamProperties;

  /** Returns the viewer's account state for the request's Client Identity. */
  @Transactional
  public AccountMeResponse getCurrentAccount(String clientId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      AccountUser user = ensureIamUser(jwtAuth.getToken());
      return buildAuthenticatedResponse(clientId, user.getId().value(), user.getEmail());
    }
    if (isAuthenticated(authentication)) {
      OAuthIdentity identity = extractIdentity(authentication);
      if (identity != null) {
        Optional<AccountUser> linked =
            accountUserRepository.findByProviderAndSubject(identity.provider(), identity.subject());
        String email = linked.map(AccountUser::getEmail).orElse(identity.email());
        String userId = linked.map(user -> user.getId().value()).orElse(identity.subject());
        return buildAuthenticatedResponse(clientId, userId, email);
      }
    }

    // Session may be missing after a host mismatch; Client Identity link still proves login.
    if (clientId != null && !clientId.isBlank()) {
      Optional<AccountUser> byClient = accountUserRepository.findByLinkedClientId(clientId);
      if (byClient.isPresent()) {
        AccountUser user = byClient.get();
        return buildAuthenticatedResponse(clientId, user.getId().value(), user.getEmail());
      }
    }

    return new AccountMeResponse(
        AccountMode.ANONYMOUS,
        clientId,
        null,
        null,
        AccountPlan.from(billingProperties.getPlan()),
        isLoginAvailable(),
        loginProviders());
  }

  /** Links OAuth identity to the browser cookie and returns the account user id. */
  @Transactional
  public String linkOAuthUser(String provider, String subject, String email, String clientId) {
    AccountUser user =
        accountUserRepository
            .findByProviderAndSubject(provider, subject)
            .orElseGet(() -> AccountUser.create(provider, subject, email, clientId));
    accountUserRepository
        .findByLinkedClientId(clientId)
        .filter(previous -> !previous.getId().equals(user.getId()))
        .ifPresent(
            previous -> {
              previous.unlinkBrowser();
              accountUserRepository.save(previous);
            });
    user.linkSession(email, clientId);
    accountUserRepository.save(user);
    return user.getId().value();
  }

  /** Clears OAuth ↔ Client Identity link so the browser returns to guest mode. */
  @Transactional
  public void unlinkClient(String clientId) {
    accountUserRepository
        .findByLinkedClientId(clientId)
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

  private AccountUser ensureIamUser(Jwt jwt) {
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("IAM JWT subject is required");
    }
    String email = jwt.getClaimAsString("email");
    return accountUserRepository
        .findByProviderAndSubject("explore-iam", subject)
        .orElseGet(
            () ->
                accountUserRepository.save(
                    AccountUser.create("explore-iam", subject, email, null)));
  }

  private AccountMeResponse buildAuthenticatedResponse(
      String clientId, String userId, String email) {
    return new AccountMeResponse(
        AccountMode.AUTHENTICATED,
        clientId,
        userId,
        email,
        AccountPlan.from(billingProperties.getPlan()),
        isLoginAvailable(),
        loginProviders());
  }

  private static boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }

  private static OAuthIdentity extractIdentity(Authentication authentication) {
    String provider = getRegistrationId(authentication);
    Object principal = authentication.getPrincipal();
    if (principal instanceof OidcUser oidcUser) {
      String subject = oidcUser.getSubject();
      if (subject == null || subject.isBlank()) {
        return null;
      }
      String email = oidcUser.getEmail();
      if (email == null || email.isBlank()) {
        email = oidcUser.getAttribute("email");
      }
      return new OAuthIdentity(provider, subject, email);
    }
    if (principal instanceof OAuth2User oauth2User) {
      String subject = oauth2User.getName();
      if (subject == null || subject.isBlank()) {
        return null;
      }
      // GitHub often omits email on /user; fall back to login/name for display.
      return new OAuthIdentity(provider, subject, resolveOAuthEmail(oauth2User));
    }
    return null;
  }

  private static String resolveOAuthEmail(OAuth2User oauth2User) {
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

  private static String getRegistrationId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken token) {
      return token.getAuthorizedClientRegistrationId();
    }
    return "unknown";
  }

  private record OAuthIdentity(String provider, String subject, String email) {}
}
