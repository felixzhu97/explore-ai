package com.ai.account.infra.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.util.StringUtils;

/**
 * JWT resource server decoder for Explore IAM access tokens (native / API Bearer).
 *
 * <p>Uses {@link SupplierJwtDecoder} so issuer discovery runs on first token validation, not at
 * application startup (local boot without IAM stays healthy).
 *
 * @see <a
 *     href="https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html">JWT
 *     Resource Server</a>
 */
@Configuration
@EnableConfigurationProperties(OAuthExploreIamProperties.class)
public class IamResourceServerConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "app.oauth.explore-iam",
      name = "resource-server-enabled",
      havingValue = "true",
      matchIfMissing = true)
  JwtDecoder iamJwtDecoder(OAuthExploreIamProperties properties) {
    if (!StringUtils.hasText(properties.getIssuerUri())) {
      throw new IllegalStateException(
          "app.oauth.explore-iam.issuer-uri is required when resource-server-enabled=true");
    }
    String issuer = properties.getIssuerUri().trim();
    return new SupplierJwtDecoder(() -> JwtDecoders.fromIssuerLocation(issuer));
  }
}
