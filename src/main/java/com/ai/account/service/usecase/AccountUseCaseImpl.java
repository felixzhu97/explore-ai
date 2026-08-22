package com.ai.account.service.usecase;

import com.ai.account.controller.dto.AccountMeResponse;
import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.infra.config.OAuthExploreIamProperties;
import com.ai.account.infra.config.OAuthGithubProperties;
import com.ai.account.infra.config.OAuthGoogleProperties;
import com.ai.billing.infra.config.BillingProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Documentation. */
@Service
@EnableConfigurationProperties({
  BillingProperties.class,
  OAuthGoogleProperties.class,
  OAuthGithubProperties.class,
  OAuthExploreIamProperties.class
})
public class AccountUseCaseImpl implements AccountUseCase {

  private final AccountUserRepository accountUserRepository;
  private final BillingProperties billingProperties;
  private final OAuthGoogleProperties oauthGoogleProperties;
  private final OAuthGithubProperties oauthGithubProperties;
  private final OAuthExploreIamProperties oauthExploreIamProperties;

  /** Documentation. */
  public AccountUseCaseImpl(
      AccountUserRepository accountUserRepository,
      BillingProperties billingProperties,
      OAuthGoogleProperties oauthGoogleProperties,
      OAuthGithubProperties oauthGithubProperties,
      OAuthExploreIamProperties oauthExploreIamProperties) {
    this.accountUserRepository = accountUserRepository;
    this.billingProperties = billingProperties;
    this.oauthGoogleProperties = oauthGoogleProperties;
    this.oauthGithubProperties = oauthGithubProperties;
    this.oauthExploreIamProperties = oauthExploreIamProperties;
  }

  @Override
  public AccountMeResponse currentAccount(String clientId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isAuthenticated(authentication)) {
      OAuthIdentity identity = extractIdentity(authentication);
      if (identity != null) {
        Optional<AccountUser> linked =
            accountUserRepository.findByProviderAndSubject(identity.provider(), identity.subject());
        String email = linked.map(AccountUser::getEmail).orElse(identity.email());
        String userId = linked.map(AccountUser::getId).orElse(identity.subject());
        return authenticated(clientId, userId, email);
      }
    }

    // Session may be missing after a host mismatch; Client Identity link still proves login.
    Optional<AccountUser> byClient = accountUserRepository.findByLinkedClientId(clientId);
    if (byClient.isPresent()) {
      AccountUser user = byClient.get();
      return authenticated(clientId, user.getId(), user.getEmail());
    }

    return new AccountMeResponse(
        "anonymous",
        clientId,
        null,
        null,
        billingProperties.getPlan(),
        isLoginAvailable(),
        loginProviders());
  }

  @Override
  @Transactional
  public String linkOAuthUser(String provider, String subject, String email, String clientId) {
    AccountUser user =
        accountUserRepository
            .findByProviderAndSubject(provider, subject)
            .orElseGet(() -> AccountUser.create(provider, subject, email, clientId));
    user.linkSession(email, clientId);
    accountUserRepository.save(user);
    return user.getId();
  }

  @Override
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

  @Override
  public boolean isLoginAvailable() {
    return !loginProviders().isEmpty();
  }

  @Override
  public List<String> loginProviders() {
    List<String> providers = new ArrayList<>(3);
    if (oauthGoogleProperties.isReady()) {
      providers.add("google");
    }
    if (oauthGithubProperties.isReady()) {
      providers.add("github");
    }
    if (oauthExploreIamProperties.isReady()) {
      providers.add("explore-iam");
    }
    return List.copyOf(providers);
  }

  private AccountMeResponse authenticated(String clientId, String userId, String email) {
    return new AccountMeResponse(
        "authenticated",
        clientId,
        userId,
        email,
        billingProperties.getPlan(),
        isLoginAvailable(),
        loginProviders());
  }

  private static boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }

  private static OAuthIdentity extractIdentity(Authentication authentication) {
    String provider = registrationId(authentication);
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

  private static String registrationId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken token) {
      return token.getAuthorizedClientRegistrationId();
    }
    return "unknown";
  }

  private record OAuthIdentity(String provider, String subject, String email) {}
}
