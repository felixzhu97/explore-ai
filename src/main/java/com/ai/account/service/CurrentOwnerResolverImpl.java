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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves data {@link OwnerKey} from guest Client Identity and/or OAuth / IAM JWT. */
@Service
public class CurrentOwnerResolverImpl implements CurrentOwnerResolver {

  public static final String EXPLORE_IAM_PROVIDER = "explore-iam";

  private final AccountUserRepository accountUserRepository;

  public CurrentOwnerResolverImpl(AccountUserRepository accountUserRepository) {
    this.accountUserRepository = accountUserRepository;
  }

  @Override
  @Transactional
  public OwnerKey resolve(String clientId, Authentication authentication) {
    Optional<AccountUser> fromAuth = resolveLinkedUser(authentication);
    if (fromAuth.isPresent()) {
      return OwnerKey.forAccount(fromAuth.get().getId().value());
    }

    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("clientId is required");
    }

    return accountUserRepository
        .findByLinkedClientId(clientId.trim())
        .map(user -> OwnerKey.forAccount(user.getId().value()))
        .orElseGet(() -> OwnerKey.forClient(clientId));
  }

  @Override
  @Transactional
  public OwnerKey resolveFromJwt(Jwt jwt) {
    AccountUser user = ensureIamUser(jwt);
    return OwnerKey.forAccount(user.getId().value());
  }

  private Optional<AccountUser> resolveLinkedUser(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return Optional.empty();
    }
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return Optional.of(ensureIamUser(jwtAuth.getToken()));
    }
    String provider = registrationId(authentication);
    String subject = subject(authentication);
    if (subject == null || subject.isBlank()) {
      return Optional.empty();
    }
    return accountUserRepository.findByProviderAndSubject(provider, subject);
  }

  private AccountUser ensureIamUser(Jwt jwt) {
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("IAM JWT subject is required");
    }
    String email = jwt.getClaimAsString("email");
    return accountUserRepository
        .findByProviderAndSubject(EXPLORE_IAM_PROVIDER, subject)
        .orElseGet(
            () ->
                accountUserRepository.save(
                    AccountUser.create(EXPLORE_IAM_PROVIDER, subject, email, null)));
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
