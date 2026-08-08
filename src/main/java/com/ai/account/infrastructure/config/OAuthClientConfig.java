package com.ai.account.infrastructure.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
@EnableConfigurationProperties(OAuthGoogleProperties.class)
public class OAuthClientConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.oauth.google", name = "enabled", havingValue = "true")
    ClientRegistrationRepository googleClientRegistrationRepository(OAuthGoogleProperties properties) {
        if (properties.getClientId() == null
                || properties.getClientId().isBlank()
                || properties.getClientSecret() == null
                || properties.getClientSecret().isBlank()) {
            throw new IllegalStateException(
                    "app.oauth.google.enabled=true requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET");
        }
        String redirectUri = properties.getRedirectUri();
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";
        }
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(List.of(google));
    }
}
