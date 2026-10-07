package com.ai.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.account.controller.dto.AccountMode;
import com.ai.account.controller.dto.LoginProvider;
import com.ai.account.domain.model.Account;
import com.ai.account.domain.model.ClientId;
import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.model.ExternalIdentity;
import com.ai.account.domain.repository.AccountRepository;
import com.ai.account.infra.config.OAuthExploreIamProperties;
import com.ai.account.infra.config.OAuthGithubProperties;
import com.ai.account.infra.config.OAuthGoogleProperties;
import com.ai.billing.infra.config.BillingProperties;
import com.ai.billing.service.BillingPlanService;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

  private static final String CID_1 = "11111111-1111-1111-1111-111111111111";
  private static final String CID_2 = "22222222-2222-2222-2222-222222222222";
  private static final String CID_GH = "33333333-3333-3333-3333-333333333333";
  private static final String CID_SHARED = "44444444-4444-4444-4444-444444444444";

  @Mock private AccountRepository accountRepository;

  private AccountService useCase;
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
        new AccountService(
            accountRepository,
            new IamAccountService(accountRepository),
            new BillingPlanService(billing),
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
    var response = useCase.getCurrentAccount(CID_1);

    assertThat(response.mode()).isEqualTo(AccountMode.ANONYMOUS);
    assertThat(response.clientId()).isEqualTo(CID_1);
    assertThat(response.loginAvailable()).isFalse();
    assertThat(response.loginProviders()).isEmpty();
  }

  @Test
  void shouldReturnAnonymousWhenAnonymousAuthenticationToken() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    var response = useCase.getCurrentAccount(CID_1);

    assertThat(response.mode()).isEqualTo(AccountMode.ANONYMOUS);
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
    Account linked = account("google", "sub-1", "user@example.com", CID_1);
    when(accountRepository.findByIdentity(ExternalIdentity.createIdentity("google", "sub-1")))
        .thenReturn(Optional.of(linked));

    var response = useCase.getCurrentAccount(CID_1);

    assertThat(response.mode()).isEqualTo(AccountMode.AUTHENTICATED);
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.userId()).isEqualTo(linked.getId().toString());
    assertThat(response.loginAvailable()).isTrue();
    assertThat(response.loginProviders()).containsExactly(LoginProvider.GOOGLE);
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
    Account linked = account("github", "42", "octocat@github.com", CID_GH);
    when(accountRepository.findByIdentity(ExternalIdentity.createIdentity("github", "42")))
        .thenReturn(Optional.of(linked));

    var response = useCase.getCurrentAccount(CID_GH);

    assertThat(response.mode()).isEqualTo(AccountMode.AUTHENTICATED);
    assertThat(response.email()).isEqualTo("octocat@github.com");
    assertThat(response.loginProviders()).containsExactly(LoginProvider.GITHUB);
  }

  @Test
  @DisplayName(
      "should show the github login as display name and not as email when email is private")
  void shouldShowTheGithubLoginAsDisplayNameAndNotAsEmailWhenEmailIsPrivate() {
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
    Account linked = account("github", "42", null, "octocat", CID_GH);
    when(accountRepository.findByIdentity(ExternalIdentity.createIdentity("github", "42")))
        .thenReturn(Optional.of(linked));

    var response = useCase.getCurrentAccount(CID_GH);

    assertThat(response.mode()).isEqualTo(AccountMode.AUTHENTICATED);
    assertThat(response.email()).isNull();
    assertThat(response.displayName()).isEqualTo("octocat");
  }

  @Test
  void shouldLinkOAuthUserWhenNewSubject() {
    when(accountRepository.findByIdentity(ExternalIdentity.createIdentity("google", "sub-9")))
        .thenReturn(Optional.empty());

    when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Account account = useCase.linkOAuthUser(signIn("google", "sub-9", "a@b.com"), client(CID_1));

    assertThat(account.getLinkedClientId()).isEqualTo(client(CID_1));
  }

  @Test
  @DisplayName("should unlink the previous account when another account signs in on the browser")
  void shouldUnlinkThePreviousAccountWhenAnotherAccountSignsInOnTheBrowser() {
    Account previous = account("google", "sub-a", "a@example.com", CID_SHARED);
    when(accountRepository.findByIdentity(ExternalIdentity.createIdentity("github", "sub-b")))
        .thenReturn(Optional.empty());
    when(accountRepository.findByLinkedClientId(ClientId.parseId(CID_SHARED)))
        .thenReturn(Optional.of(previous));

    useCase.linkOAuthUser(signIn("github", "sub-b", "b@example.com"), client(CID_SHARED));

    assertThat(previous.getLinkedClientId()).isNull();
    verify(accountRepository).save(previous);
  }

  @Test
  @DisplayName("should keep the link when the same account signs in again")
  void shouldKeepTheLinkWhenTheSameAccountSignsInAgain() {
    Account account = account("google", "sub-a", "a@example.com", CID_SHARED);
    when(accountRepository.findByIdentity(ExternalIdentity.createIdentity("google", "sub-a")))
        .thenReturn(Optional.of(account));
    when(accountRepository.findByLinkedClientId(ClientId.parseId(CID_SHARED)))
        .thenReturn(Optional.of(account));

    useCase.linkOAuthUser(signIn("google", "sub-a", "a@example.com"), client(CID_SHARED));

    assertThat(account.getLinkedClientId()).isEqualTo(client(CID_SHARED));
  }

  @Test
  void shouldReturnAuthenticatedWhenLinkedClientIdPresentWithoutSecurityContext() {
    Account linked = account("google", "sub-2", "u@example.com", CID_2);
    when(accountRepository.findByLinkedClientId(ClientId.parseId(CID_2)))
        .thenReturn(Optional.of(linked));

    var response = useCase.getCurrentAccount(CID_2);

    assertThat(response.mode()).isEqualTo(AccountMode.AUTHENTICATED);
    assertThat(response.email()).isEqualTo("u@example.com");
  }

  @Test
  void shouldReportLoginAvailableWhenGoogleConfigured() {
    oauthGoogleProperties.setEnabled(true);
    oauthGoogleProperties.setClientId("id");
    oauthGoogleProperties.setClientSecret("secret");

    assertThat(useCase.isLoginAvailable()).isTrue();
    assertThat(useCase.loginProviders()).isEqualTo(List.of(LoginProvider.GOOGLE));
  }

  @Test
  void shouldReportBothProvidersWhenGoogleAndGithubConfigured() {
    oauthGoogleProperties.setEnabled(true);
    oauthGoogleProperties.setClientId("g");
    oauthGoogleProperties.setClientSecret("gs");
    oauthGithubProperties.setEnabled(true);
    oauthGithubProperties.setClientId("h");
    oauthGithubProperties.setClientSecret("hs");

    assertThat(useCase.loginProviders())
        .containsExactly(LoginProvider.GOOGLE, LoginProvider.GITHUB);
  }

  @Test
  void shouldIncludeExploreIamWhenExploreIamConfigured() {
    oauthExploreIamProperties.setEnabled(true);
    oauthExploreIamProperties.setClientId("explore-ai");
    oauthExploreIamProperties.setClientSecret("secret");
    oauthExploreIamProperties.setIssuerUri("http://localhost:9100");

    assertThat(useCase.isLoginAvailable()).isTrue();
    assertThat(useCase.loginProviders()).containsExactly(LoginProvider.EXPLORE_IAM);
  }

  @Test
  void shouldReturnAuthenticatedWhenIamJwtPresent() {
    Jwt jwt =
        Jwt.withTokenValue("iam-token")
            .header("alg", "none")
            .subject("iam-sub-1")
            .claim("email", "iam@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER")));
    Account linked = account("explore-iam", "iam-sub-1", "iam@example.com", null);
    when(accountRepository.findByIdentity(
            ExternalIdentity.createIdentity("explore-iam", "iam-sub-1")))
        .thenReturn(Optional.of(linked));
    when(accountRepository.save(linked)).thenReturn(linked);

    var response = useCase.getCurrentAccount(null);

    assertThat(response.mode()).isEqualTo(AccountMode.AUTHENTICATED);
    assertThat(response.email()).isEqualTo("iam@example.com");
    assertThat(response.userId()).isEqualTo(linked.getId().toString());
    assertThat(response.clientId()).isNull();
  }

  @Test
  void shouldCreateAccountWhenIamJwtSubjectIsNew() {
    Jwt jwt =
        Jwt.withTokenValue("iam-token")
            .header("alg", "none")
            .subject("iam-new")
            .claim("email", "new@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER")));
    when(accountRepository.findByIdentity(
            ExternalIdentity.createIdentity("explore-iam", "iam-new")))
        .thenReturn(Optional.empty());
    when(accountRepository.save(org.mockito.ArgumentMatchers.any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = useCase.getCurrentAccount(null);

    assertThat(response.mode()).isEqualTo(AccountMode.AUTHENTICATED);
    assertThat(response.email()).isEqualTo("new@example.com");
  }

  private static Account account(String provider, String subject, String email, String clientId) {
    return account(provider, subject, email, null, clientId);
  }

  private static Account account(
      String provider, String subject, String email, String displayName, String clientId) {
    Account account =
        Account.createAccount(
            ExternalIdentity.createIdentity(provider, subject),
            ContactEmail.parseOptionalEmail(email),
            displayName);
    if (clientId != null) {
      account.linkBrowser(client(clientId), null, null);
    }
    return account;
  }

  private static OAuthSignIn signIn(String provider, String subject, String email) {
    return new OAuthSignIn(
        ExternalIdentity.createIdentity(provider, subject),
        ContactEmail.parseOptionalEmail(email),
        null);
  }

  private static ClientId client(String raw) {
    return ClientId.parseId(raw);
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
