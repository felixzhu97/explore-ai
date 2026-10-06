package com.ai.account.service;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.domain.vo.ClientId;
import com.ai.common.domain.vo.OwnerKey;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves data {@link OwnerKey} from guest Client Identity and/or OAuth / IAM JWT. */
@Service
@RequiredArgsConstructor
public class CurrentOwnerResolver {

  private final AccountUserRepository accountUserRepository;
  private final IamAccountService iamAccountService;

  /** Resolves owner from guest Client Identity and/or OAuth / IAM JWT authentication. */
  @Transactional
  public OwnerKey resolve(String clientId, Authentication authentication) {
    Optional<AccountUser> fromAuth = resolveSignedInUser(authentication);
    if (fromAuth.isPresent()) {
      return fromAuth.get().ownerKey();
    }
    if (ClientId.isValid(clientId)) {
      Optional<AccountUser> linked =
          accountUserRepository.findByLinkedClientId(ClientId.parse(clientId));
      if (linked.isPresent()) {
        return linked.get().ownerKey();
      }
    }
    return OwnerKey.forClient(clientId);
  }

  /**
   * Ensures an account exists for an IAM access token and returns its owner key.
   *
   * @param jwt validated IAM JWT
   * @return account owner key
   */
  @Transactional
  public OwnerKey resolveFromJwt(Jwt jwt) {
    return iamAccountService.signIn(jwt).ownerKey();
  }

  private Optional<AccountUser> resolveSignedInUser(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return Optional.of(iamAccountService.signIn(jwtAuth.getToken()));
    }
    return OAuthSignIn.from(authentication)
        .flatMap(signIn -> accountUserRepository.findByIdentity(signIn.identity()));
  }
}
