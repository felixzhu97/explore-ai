package com.ai.account.service;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.repository.AccountRepository;
import com.ai.common.domain.model.OwnerKey;
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

  private final AccountRepository accountRepository;
  private final IamAccountService iamAccountService;

  /** Resolves owner from guest Client Identity and/or OAuth / IAM JWT authentication. */
  @Transactional
  public OwnerKey resolve(String clientId, Authentication authentication) {
    Optional<Account> fromAuth = resolveSignedInUser(authentication);
    if (fromAuth.isPresent()) {
      return fromAuth.get().createOwnerKey();
    }
    if (ClientId.isValid(clientId)) {
      Optional<Account> linked = accountRepository.findByLinkedClientId(ClientId.parseId(clientId));
      if (linked.isPresent()) {
        return linked.get().createOwnerKey();
      }
    }
    return OwnerKey.createClientKey(clientId);
  }

  /**
   * Ensures an account exists for an IAM access token and returns its owner key.
   *
   * @param jwt validated IAM JWT
   * @return account owner key
   */
  @Transactional
  public OwnerKey resolveFromJwt(Jwt jwt) {
    return iamAccountService.signIn(jwt).createOwnerKey();
  }

  private Optional<Account> resolveSignedInUser(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return Optional.of(iamAccountService.signIn(jwtAuth.getToken()));
    }
    return OAuthSignIn.from(authentication)
        .flatMap(signIn -> accountRepository.findByIdentity(signIn.identity()));
  }
}
