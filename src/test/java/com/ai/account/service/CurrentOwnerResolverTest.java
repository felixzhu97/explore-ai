package com.ai.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.domain.vo.ClientId;
import com.ai.account.domain.vo.ExternalIdentity;
import com.ai.common.domain.vo.OwnerKey;
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

  @Mock private AccountUserRepository accountUserRepository;
  @Mock private IamAccountService iamAccountService;

  @InjectMocks private CurrentOwnerResolver resolver;

  @Test
  void shouldReturnClientOwnerWhenGuestWithoutLink() {
    when(accountUserRepository.findByLinkedClientId(ClientId.parse(CLIENT_ID)))
        .thenReturn(Optional.empty());

    OwnerKey key = resolver.resolve(CLIENT_ID, null);

    assertThat(key).isEqualTo(OwnerKey.forClient(CLIENT_ID));
  }

  @Test
  void shouldReturnAccountOwnerWhenLinkedClientIdPresent() {
    AccountUser user = AccountUser.create(ExternalIdentity.of("google", "sub"), null, null);
    when(accountUserRepository.findByLinkedClientId(ClientId.parse(CLIENT_ID)))
        .thenReturn(Optional.of(user));

    OwnerKey key =
        resolver.resolve(
            CLIENT_ID,
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    assertThat(key).isEqualTo(user.ownerKey());
  }

  @Test
  @DisplayName("should stay a guest without a lookup when the client id is not a UUID")
  void shouldStayAGuestWithoutALookupWhenTheClientIdIsNotAUuid() {
    OwnerKey key = resolver.resolve("cid-legacy", null);

    assertThat(key).isEqualTo(OwnerKey.forClient("cid-legacy"));
    verifyNoInteractions(accountUserRepository);
  }

  @Test
  void shouldReturnAccountOwnerWhenOAuthAuthenticated() {
    AccountUser user = AccountUser.create(ExternalIdentity.of("google", "sub-9"), null, null);
    when(accountUserRepository.findByIdentity(ExternalIdentity.of("google", "sub-9")))
        .thenReturn(Optional.of(user));

    OidcIdToken idToken =
        new OidcIdToken(
            "token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "sub-9"));
    OidcUser oidcUser =
        new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"), idToken);
    OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, List.of(), "google");

    OwnerKey key = resolver.resolve(CLIENT_ID, auth);

    assertThat(key).isEqualTo(user.ownerKey());
  }

  @Test
  void shouldReturnAccountOwnerWhenIamJwtAuthenticated() {
    AccountUser user = AccountUser.create(ExternalIdentity.iam("iam-sub"), null, null);
    Jwt jwt = IamAccountServiceTest.jwt("iam-sub", "iam@example.com");
    when(iamAccountService.signIn(jwt)).thenReturn(user);

    OwnerKey key =
        resolver.resolve(
            CLIENT_ID,
            new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER")));

    assertThat(key).isEqualTo(user.ownerKey());
    verifyNoInteractions(accountUserRepository);
  }
}
