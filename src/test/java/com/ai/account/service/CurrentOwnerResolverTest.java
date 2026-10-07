package com.ai.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountRepository;
import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrentOwnerResolver")
class CurrentOwnerResolverTest {

  private static final String CLIENT_ID = "11111111-1111-1111-1111-111111111111";

  @Mock private AccountRepository accountRepository;
  @Mock private IamAccountService iamAccountService;

  @InjectMocks private CurrentOwnerResolver resolver;

  @Test
  void shouldReturnClientOwnerWhenGuestWithoutLink() {
    when(accountRepository.findByLinkedClientId(ClientId.parseId(CLIENT_ID)))
        .thenReturn(Optional.empty());

    OwnerKey key = resolver.resolve(CLIENT_ID, null);

    assertThat(key).isEqualTo(OwnerKey.createClientKey(CLIENT_ID));
  }

  @Test
  void shouldReturnAccountOwnerWhenLinkedClientIdPresent() {
    Account account =
        Account.createAccount(ExternalIdentity.createIdentity("google", "sub"), null, null);
    when(accountRepository.findByLinkedClientId(ClientId.parseId(CLIENT_ID)))
        .thenReturn(Optional.of(account));

    OwnerKey key =
        resolver.resolve(
            CLIENT_ID,
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(key).isEqualTo(account.createOwnerKey());
  }

  @Test
  @DisplayName("should stay a guest without a lookup when the client id is not a UUID")
  void shouldStayAGuestWithoutALookupWhenTheClientIdIsNotAUuid() {
    OwnerKey key = resolver.resolve("cid-legacy", null);

    assertThat(key).isEqualTo(OwnerKey.createClientKey("cid-legacy"));
    verifyNoInteractions(accountRepository);
  }

  @Test
  void shouldReturnAccountOwnerWhenOAuthAuthenticated() {
    Account account =
        Account.createAccount(ExternalIdentity.createIdentity("google", "sub-9"), null, null);
    when(accountRepository.findByIdentity(ExternalIdentity.createIdentity("google", "sub-9")))
        .thenReturn(Optional.of(account));

    OidcIdToken idToken =
        new OidcIdToken(
            "token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "sub-9"));
    OidcUser oidcUser =
        new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"), idToken);
    OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, List.of(), "google");

    OwnerKey key = resolver.resolve(CLIENT_ID, auth);

    assertThat(key).isEqualTo(account.createOwnerKey());
  }

  @Test
  void shouldReturnAccountOwnerWhenIamJwtAuthenticated() {
    Account account =
        Account.createAccount(ExternalIdentity.createIamIdentity("iam-sub"), null, null);
    Jwt jwt = IamAccountServiceTest.jwt("iam-sub", "iam@example.com");
    when(iamAccountService.signIn(jwt)).thenReturn(account);

    OwnerKey key =
        resolver.resolve(
            CLIENT_ID,
            new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER")));

    assertThat(key).isEqualTo(account.createOwnerKey());
    verifyNoInteractions(accountRepository);
  }
}
