package com.ai.account.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Builds a single {@link ClientRegistrationRepository} for every enabled OAuth provider.
 *
 * @see <a
 *     href="https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html">OAuth2
 *     Login</a>
 * @see <a
 *     href="https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps">GitHub
 *     OAuth Apps</a>
 */
@Configuration
@EnableConfigurationProperties({
  OAuthGoogleProperties.class,
  OAuthGithubProperties.class,
  OAuthExploreIamProperties.class,
  OAuthSpaProperties.class
})
public class OAuthClientConfig {

  private static final String DEFAULT_REDIRECT = "{baseUrl}/login/oauth2/code/{registrationId}";

  @Bean
  @Conditional(AnyOAuthProviderReadyCondition.class)
  ClientRegistrationRepository oauthClientRegistrationRepository(
      OAuthGoogleProperties googleProperties,
      OAuthGithubProperties githubProperties,
      OAuthExploreIamProperties exploreIamProperties) {
    List<ClientRegistration> registrations = new ArrayList<>();
    if (googleProperties.isReady()) {
      registrations.add(googleRegistration(googleProperties));
    }
    if (githubProperties.isReady()) {
      registrations.add(githubRegistration(githubProperties));
    }
    if (exploreIamProperties.isReady()) {
      registrations.add(exploreIamRegistration(exploreIamProperties));
    }
    if (registrations.isEmpty()) {
      throw new IllegalStateException("OAuth enabled but no provider has client credentials ready");
    }
    return new InMemoryClientRegistrationRepository(registrations);
  }

  private static ClientRegistration googleRegistration(OAuthGoogleProperties properties) {
    return ClientRegistration.withRegistrationId("google")
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(redirectOrDefault(properties.getRedirectUri()))
        .scope("openid", "profile", "email")
        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
        .tokenUri("https://oauth2.googleapis.com/token")
        .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
        .userNameAttributeName(IdTokenClaimNames.SUB)
        .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
        .clientName("Google")
        .build();
  }

  private static ClientRegistration githubRegistration(OAuthGithubProperties properties) {
    return CommonOAuth2Provider.GITHUB
        .getBuilder("github")
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .redirectUri(redirectOrDefault(properties.getRedirectUri()))
        .scope("read:user", "user:email")
        .build();
  }

  private static ClientRegistration exploreIamRegistration(OAuthExploreIamProperties properties) {
    return ClientRegistrations.fromIssuerLocation(properties.getIssuerUri())
        .registrationId("explore-iam")
        .clientId(properties.getClientId())
        .clientSecret(properties.getClientSecret())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(redirectOrDefault(properties.getRedirectUri()))
        .scope("openid", "profile", "email")
        .userNameAttributeName(IdTokenClaimNames.SUB)
        .clientName("Explore IAM")
        .build();
  }

  private static String redirectOrDefault(String configured) {
    if (configured == null || configured.isBlank()) {
      return DEFAULT_REDIRECT;
    }
    return configured;
  }
}
