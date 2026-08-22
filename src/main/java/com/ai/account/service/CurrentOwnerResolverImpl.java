package com.ai.account.service;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.common.domain.vo.OwnerKey;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/** Documentation. */
@Service
public class CurrentOwnerResolverImpl implements CurrentOwnerResolver {

  private final AccountUserRepository accountUserRepository;

  /** Documentation. */
  public CurrentOwnerResolverImpl(AccountUserRepository accountUserRepository) {
    this.accountUserRepository = accountUserRepository;
  }

  @Override
  public OwnerKey resolve(String clientId, Authentication authentication) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("clientId is required");
    }

    Optional<AccountUser> fromAuth = resolveLinkedUser(authentication);
    if (fromAuth.isPresent()) {
      return OwnerKey.forAccount(fromAuth.get().getId().value());
    }

    return accountUserRepository
        .findByLinkedClientId(clientId.trim())
        .map(user -> OwnerKey.forAccount(user.getId().value()))
        .orElseGet(() -> OwnerKey.forClient(clientId));
  }

  private Optional<AccountUser> resolveLinkedUser(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return Optional.empty();
    }
    String provider = registrationId(authentication);
    String subject = subject(authentication);
    if (subject == null || subject.isBlank()) {
      return Optional.empty();
    }
    return accountUserRepository.findByProviderAndSubject(provider, subject);
  }

  private static String registrationId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken token) {
      return token.getAuthorizedClientRegistrationId();
    }
    return "unknown";
  }

  private static String subject(Authentication authentication) {
    Object principal = authentication.getPrincipal();
    if (principal instanceof OidcUser oidcUser) {
      return oidcUser.getSubject();
    }
    if (principal instanceof OAuth2User oauth2User) {
      return oauth2User.getName();
    }
    return null;
  }
}
