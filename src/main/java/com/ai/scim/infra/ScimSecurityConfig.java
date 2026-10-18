package com.ai.scim.infra;

import com.unboundid.scim2.common.messages.ErrorResponse;
import com.unboundid.scim2.common.utils.JsonUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless Bearer chain for {@code /scim/v2/**} that requires {@code write:ai_scim} on an Explore
 * IAM token. Ordered before the session chains; without an IAM JWT decoder every SCIM call is
 * refused.
 */
@Configuration
public class ScimSecurityConfig {

  /** Authority granted for the {@code write:ai_scim} scope. */
  public static final String SCIM_AUTHORITY = "SCOPE_write:ai_scim";

  @Bean
  @Order(0)
  SecurityFilterChain scimSecurityFilterChain(
      HttpSecurity http,
      ObjectProvider<JwtDecoder> jwtDecoder,
      JwtAuthenticationConverter iamJwtAuthenticationConverter)
      throws Exception {
    BearerTokenAuthenticationEntryPoint bearerEntryPoint =
        new BearerTokenAuthenticationEntryPoint();
    BearerTokenAccessDeniedHandler bearerDenied = new BearerTokenAccessDeniedHandler();
    boolean resourceServer = jwtDecoder.getIfAvailable() != null;
    http.securityMatcher("/scim/v2/**")
        .authorizeHttpRequests(
            authorize -> {
              if (resourceServer) {
                authorize.anyRequest().hasAuthority(SCIM_AUTHORITY);
              } else {
                authorize.anyRequest().denyAll();
              }
            })
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Bearer-only API without cookies, so CSRF does not apply.
        // codeql[java/spring-disabled-csrf-protection]
        .csrf(AbstractHttpConfigurer::disable)
        .headers(Customizer.withDefaults())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, failure) -> {
                          bearerEntryPoint.commence(request, response, failure);
                          writeError(response, 401, "authentication required");
                        })
                    .accessDeniedHandler(
                        (request, response, denied) -> {
                          bearerDenied.handle(request, response, denied);
                          writeError(response, 403, "insufficient scope");
                        }));
    if (resourceServer) {
      http.oauth2ResourceServer(
          oauth2 ->
              oauth2
                  .jwt(jwt -> jwt.jwtAuthenticationConverter(iamJwtAuthenticationConverter))
                  .authenticationEntryPoint(
                      (request, response, failure) -> {
                        bearerEntryPoint.commence(request, response, failure);
                        writeError(response, 401, "authentication required");
                      }));
    }
    return http.build();
  }

  private static void writeError(HttpServletResponse response, int status, String detail)
      throws IOException {
    ErrorResponse error = ScimErrors.error(status, null, detail);
    response.setStatus(status);
    response.setContentType(ScimJsonHttpMessageConverter.SCIM_JSON_VALUE);
    JsonUtils.createObjectMapper().writeValue(response.getOutputStream(), error);
  }
}
