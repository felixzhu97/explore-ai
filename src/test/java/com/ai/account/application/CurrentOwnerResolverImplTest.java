package com.ai.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrentOwnerResolverImpl")
class CurrentOwnerResolverImplTest {

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private CurrentOwnerResolverImpl resolver;

    @Test
    void should_returnClientOwner_whenGuestWithoutLink() {
        when(accountUserRepository.findByLinkedClientId("cid-1")).thenReturn(Optional.empty());

        OwnerKey key = resolver.resolve("cid-1", null);

        assertThat(key).isEqualTo(OwnerKey.forClient("cid-1"));
    }

    @Test
    void should_returnAccountOwner_whenLinkedClientIdPresent() {
        AccountUser user = AccountUser.restore(
                "acct-1", "google", "sub", "a@b.com", "cid-1", Instant.now(), Instant.now());
        when(accountUserRepository.findByLinkedClientId("cid-1")).thenReturn(Optional.of(user));

        OwnerKey key = resolver.resolve(
                "cid-1",
                new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(key).isEqualTo(OwnerKey.forAccount("acct-1"));
    }

    @Test
    void should_returnAccountOwner_whenOAuthAuthenticated() {
        AccountUser user = AccountUser.restore(
                "acct-9", "google", "sub-9", "a@b.com", "cid-9", Instant.now(), Instant.now());
        when(accountUserRepository.findByProviderAndSubject("google", "sub-9"))
                .thenReturn(Optional.of(user));

        OidcIdToken idToken = new OidcIdToken(
                "token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "sub-9"));
        OidcUser oidcUser = new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"), idToken);
        OAuth2AuthenticationToken auth =
                new OAuth2AuthenticationToken(oidcUser, List.of(), "google");

        OwnerKey key = resolver.resolve("cid-other", auth);

        assertThat(key).isEqualTo(OwnerKey.forAccount("acct-9"));
    }
}
