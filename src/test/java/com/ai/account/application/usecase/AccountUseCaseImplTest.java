package com.ai.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.infrastructure.config.OAuthExploreIamProperties;
import com.ai.account.infrastructure.config.OAuthGithubProperties;
import com.ai.account.infrastructure.config.OAuthGoogleProperties;
import com.ai.billing.infrastructure.config.BillingProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountUseCaseImpl")
class AccountUseCaseImplTest {

  @Mock private AccountUserRepository accountUserRepository;

  private AccountUseCaseImpl useCase;
  private OAuthGoogleProperties oauthGoogleProperties;
  private OAuthGithubProperties oauthGithubProperties;
  private OAuthExploreIamProperties oauthExploreIamProperties;

  @BeforeEach
  void setUp() {
    BillingProperties billing = new BillingProperties();
    billing.setPlan("free");
    oauthGoogleProperties = new OAuthGoogleProperties();
    oauthGoogleProperties.setEnabled(false);
    oauthGithubProperties = new OAuthGithubProperties();
    oauthGithubProperties.setEnabled(false);
    oauthExploreIamProperties = new OAuthExploreIamProperties();
    oauthExploreIamProperties.setEnabled(false);
    useCase =
        new AccountUseCaseImpl(
            accountUserRepository,
            billing,
            oauthGoogleProperties,
            oauthGithubProperties,
            oauthExploreIamProperties);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnAnonymousWhenNoAuthentication() {
    var response = useCase.currentAccount("cid-1");

    assertThat(response.mode()).isEqualTo("anonymous");
    assertThat(response.clientId()).isEqualTo("cid-1");
    assertThat(response.loginAvailable()).isFalse();
    assertThat(response.loginProviders()).isEmpty();
  }

  @Test
  void shouldReturnAnonymousWhenAnonymousAuthenticationToken() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    var response = useCase.currentAccount("cid-1");

    assertThat(response.mode()).isEqualTo("anonymous");
  }

  @Test
  void shouldReturnAuthenticatedWhenOidcUserPresent() {
    oauthGoogleProperties.setEnabled(true);
    oauthGoogleProperties.setClientId("cid");
    oauthGoogleProperties.setClientSecret("secret");
    OidcUser oidcUser = oidcUser("sub-1", "user@example.com");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google"));
    AccountUser linked = AccountUser.create("google", "sub-1", "user@example.com", "cid-1");
    when(accountUserRepository.findByProviderAndSubject("google", "sub-1"))
        .thenReturn(Optional.of(linked));

    var response = useCase.currentAccount("cid-1");

    assertThat(response.mode()).isEqualTo("authenticated");
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.userId()).isEqualTo(linked.getId());
    assertThat(response.loginAvailable()).isTrue();
    assertThat(response.loginProviders()).containsExactly("google");
  }

  @Test
  void shouldReturnAuthenticatedWhenGithubOAuth2UserPresent() {
    oauthGithubProperties.setEnabled(true);
    oauthGithubProperties.setClientId("gh-id");
    oauthGithubProperties.setClientSecret("gh-secret");
    OAuth2User githubUser =
        new DefaultOAuth2User(
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            Map.of("id", "42", "login", "octocat", "email", "octocat@github.com"),
            "id");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new OAuth2AuthenticationToken(githubUser, githubUser.getAuthorities(), "github"));
    AccountUser linked = AccountUser.create("github", "42", "octocat@github.com", "cid-gh");
    when(accountUserRepository.findByProviderAndSubject("github", "42"))
        .thenReturn(Optional.of(linked));

    var response = useCase.currentAccount("cid-gh");

    assertThat(response.mode()).isEqualTo("authenticated");
    assertThat(response.email()).isEqualTo("octocat@github.com");
    assertThat(response.loginProviders()).containsExactly("github");
  }

  @Test
  void shouldUseGithubLoginWhenEmailAttributeMissing() {
    oauthGithubProperties.setEnabled(true);
    oauthGithubProperties.setClientId("gh-id");
    oauthGithubProperties.setClientSecret("gh-secret");
    OAuth2User githubUser =
        new DefaultOAuth2User(
            AuthorityUtils.createAuthorityList("ROLE_USER"),
            Map.of("id", "42", "login", "octocat"),
            "id");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new OAuth2AuthenticationToken(githubUser, githubUser.getAuthorities(), "github"));
    AccountUser linked = AccountUser.create("github", "42", null, "cid-gh");
    when(accountUserRepository.findByProviderAndSubject("github", "42"))
        .thenReturn(Optional.of(linked));

    var response = useCase.currentAccount("cid-gh");

    assertThat(response.mode()).isEqualTo("authenticated");
    assertThat(response.email()).isEqualTo("octocat");
  }

  @Test
  void shouldLinkOAuthUserWhenNewSubject() {
    when(accountUserRepository.findByProviderAndSubject("google", "sub-9"))
        .thenReturn(Optional.empty());

    useCase.linkOAuthUser("google", "sub-9", "a@b.com", "cid-9");

    verify(accountUserRepository).save(org.mockito.ArgumentMatchers.any(AccountUser.class));
  }

  @Test
  void shouldReturnAuthenticatedWhenLinkedClientIdPresentWithoutSecurityContext() {
    AccountUser linked = AccountUser.create("google", "sub-2", "u@example.com", "cid-2");
    when(accountUserRepository.findByLinkedClientId("cid-2")).thenReturn(Optional.of(linked));

    var response = useCase.currentAccount("cid-2");

    assertThat(response.mode()).isEqualTo("authenticated");
    assertThat(response.email()).isEqualTo("u@example.com");
  }

  @Test
  void shouldReportLoginAvailableWhenGoogleConfigured() {
    oauthGoogleProperties.setEnabled(true);
    oauthGoogleProperties.setClientId("id");
    oauthGoogleProperties.setClientSecret("secret");

    assertThat(useCase.isLoginAvailable()).isTrue();
    assertThat(useCase.loginProviders()).isEqualTo(List.of("google"));
  }

  @Test
  void shouldReportBothProvidersWhenGoogleAndGithubConfigured() {
    oauthGoogleProperties.setEnabled(true);
    oauthGoogleProperties.setClientId("g");
    oauthGoogleProperties.setClientSecret("gs");
    oauthGithubProperties.setEnabled(true);
    oauthGithubProperties.setClientId("h");
    oauthGithubProperties.setClientSecret("hs");

    assertThat(useCase.loginProviders()).containsExactly("google", "github");
  }

  @Test
  void shouldIncludeExploreIamWhenExploreIamConfigured() {
    oauthExploreIamProperties.setEnabled(true);
    oauthExploreIamProperties.setClientId("explore-ai");
    oauthExploreIamProperties.setClientSecret("secret");
    oauthExploreIamProperties.setIssuerUri("http://localhost:9100");

    assertThat(useCase.isLoginAvailable()).isTrue();
    assertThat(useCase.loginProviders()).containsExactly("explore-iam");
  }

  private static OidcUser oidcUser(String subject, String email) {
    OidcIdToken idToken =
        new OidcIdToken(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("sub", subject, "email", email));
    return new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"), idToken);
  }
}
