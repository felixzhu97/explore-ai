package com.ai.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.infrastructure.config.OAuthGoogleProperties;
import com.ai.billing.infrastructure.config.BillingProperties;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountUseCaseImpl")
class AccountUseCaseImplTest {

    @Mock
    private AccountUserRepository accountUserRepository;

    private AccountUseCaseImpl useCase;
    private OAuthGoogleProperties oauthProperties;

    @BeforeEach
    void setUp() {
        BillingProperties billing = new BillingProperties();
        billing.setPlan("free");
        oauthProperties = new OAuthGoogleProperties();
        oauthProperties.setEnabled(false);
        useCase = new AccountUseCaseImpl(accountUserRepository, billing, oauthProperties);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_returnAnonymous_whenNoAuthentication() {
        var response = useCase.currentAccount("cid-1");

        assertThat(response.mode()).isEqualTo("anonymous");
        assertThat(response.clientId()).isEqualTo("cid-1");
        assertThat(response.loginAvailable()).isFalse();
    }

    @Test
    void should_returnAnonymous_whenAnonymousAuthenticationToken() {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        var response = useCase.currentAccount("cid-1");

        assertThat(response.mode()).isEqualTo("anonymous");
    }

    @Test
    void should_returnAuthenticated_whenOidcUserPresent() {
        oauthProperties.setEnabled(true);
        oauthProperties.setClientId("cid");
        oauthProperties.setClientSecret("secret");
        OidcUser oidcUser = oidcUser("sub-1", "user@example.com");
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        oidcUser, null, oidcUser.getAuthorities()));
        AccountUser linked = AccountUser.create("google", "sub-1", "user@example.com", "cid-1");
        when(accountUserRepository.findByProviderAndSubject("google", "sub-1"))
                .thenReturn(Optional.of(linked));

        var response = useCase.currentAccount("cid-1");

        assertThat(response.mode()).isEqualTo("authenticated");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.userId()).isEqualTo(linked.getId());
        assertThat(response.loginAvailable()).isTrue();
    }

    @Test
    void should_linkOAuthUser_whenNewSubject() {
        when(accountUserRepository.findByProviderAndSubject("google", "sub-9"))
                .thenReturn(Optional.empty());

        useCase.linkOAuthUser("google", "sub-9", "a@b.com", "cid-9");

        verify(accountUserRepository).save(org.mockito.ArgumentMatchers.any(AccountUser.class));
    }

    @Test
    void should_returnAuthenticated_whenLinkedClientIdPresentWithoutSecurityContext() {
        AccountUser linked = AccountUser.create("google", "sub-2", "u@example.com", "cid-2");
        when(accountUserRepository.findByLinkedClientId("cid-2")).thenReturn(Optional.of(linked));

        var response = useCase.currentAccount("cid-2");

        assertThat(response.mode()).isEqualTo("authenticated");
        assertThat(response.email()).isEqualTo("u@example.com");
    }

    @Test
    void should_reportLoginAvailable_whenGoogleConfigured() {
        oauthProperties.setEnabled(true);
        oauthProperties.setClientId("id");
        oauthProperties.setClientSecret("secret");

        assertThat(useCase.isLoginAvailable()).isTrue();
    }

    private static OidcUser oidcUser(String subject, String email) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("token")
                .claim("sub", subject)
                .claim("email", email)
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                .build();
        return new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"), idToken);
    }
}
