package com.ai.account.infra.config;

import com.ai.account.infra.oauth.AccountLogoutHandler;
import com.ai.account.infra.oauth.OAuthLoginFailureHandler;
import com.ai.account.infra.oauth.OAuthLoginSuccessHandler;
import com.ai.common.controller.CsrfProtectionFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Optional OAuth2 Login plus JWT resource server. Guest Client Identity stays open; IAM Bearer
 * tokens must include GitHub-style module scopes for gated paths.
 *
 * @see CsrfProtectionFilter
 * @see <a
 *     href="https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html">JWT
 *     Resource Server</a>
 * @see <a
 *     href="https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/scopes-for-oauth-apps">GitHub
 *     OAuth scopes</a>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  JwtAuthenticationConverter iamJwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    scopes.setAuthorityPrefix("SCOPE_");
    scopes.setAuthoritiesClaimName("scope");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(scopes);
    return converter;
  }

  @Bean
  @Order(1)
  @ConditionalOnBean(ClientRegistrationRepository.class)
  SecurityFilterChain oauthSecurityFilterChain(
      HttpSecurity http,
      OAuthLoginSuccessHandler successHandler,
      OAuthLoginFailureHandler failureHandler,
      AccountLogoutHandler accountLogoutHandler,
      SecurityContextRepository securityContextRepository,
      ObjectProvider<JwtDecoder> jwtDecoder,
      JwtAuthenticationConverter iamJwtAuthenticationConverter)
      throws Exception {
    http.securityMatcher("/**")
        .authorizeHttpRequests(SecurityConfig::authorizeModuleScopes)
        // SPA uses CsrfProtectionFilter (custom header), not Spring CSRF tokens.
        // codeql[java/spring-disabled-csrf-protection]
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .securityContext(
            context ->
                context
                    .securityContextRepository(securityContextRepository)
                    .requireExplicitSave(true))
        .oauth2Login(oauth -> oauth.successHandler(successHandler).failureHandler(failureHandler))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/account/logout")
                    .addLogoutHandler(accountLogoutHandler)
                    .logoutSuccessHandler(
                        (request, response, authentication) ->
                            response.setStatus(HttpStatus.NO_CONTENT.value()))
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("EASESSIONID", "JSESSIONID"))
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    applyJwtResourceServer(http, jwtDecoder, iamJwtAuthenticationConverter);
    return http.build();
  }

  @Bean
  @Order(2)
  @ConditionalOnMissingBean(name = "oauthSecurityFilterChain")
  SecurityFilterChain guestOnlySecurityFilterChain(
      HttpSecurity http,
      AccountLogoutHandler accountLogoutHandler,
      SecurityContextRepository securityContextRepository,
      ObjectProvider<JwtDecoder> jwtDecoder,
      JwtAuthenticationConverter iamJwtAuthenticationConverter)
      throws Exception {
    http.securityMatcher("/**")
        .authorizeHttpRequests(SecurityConfig::authorizeModuleScopes)
        // SPA uses CsrfProtectionFilter (custom header), not Spring CSRF tokens.
        // codeql[java/spring-disabled-csrf-protection]
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .securityContext(context -> context.securityContextRepository(securityContextRepository))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/account/logout")
                    .addLogoutHandler(accountLogoutHandler)
                    .logoutSuccessHandler(
                        (request, response, authentication) ->
                            response.setStatus(HttpStatus.NO_CONTENT.value()))
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("EASESSIONID", "JSESSIONID"));
    applyJwtResourceServer(http, jwtDecoder, iamJwtAuthenticationConverter);
    return http.build();
  }

  private static void authorizeModuleScopes(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {
    auth.requestMatchers("/api/chat", "/api/sessions/**", "/api/text/**", "/api/privacy/**")
        .access(JwtPresentScopeAuthorization.requireScope("write:ai_chat"))
        .requestMatchers("/api/audio/**", "/api/tts/**")
        .access(JwtPresentScopeAuthorization.requireScope("write:ai_audio"))
        .requestMatchers("/api/rag/**")
        .access(JwtPresentScopeAuthorization.requireScope("write:ai_rag"))
        .requestMatchers("/api/images/**", "/api/vision/**")
        .access(JwtPresentScopeAuthorization.requireScope("write:ai_media"))
        .requestMatchers(
            "/api/pipelines/**", "/api/workflows/**", "/api/automations/**", "/api/skills/**")
        .access(JwtPresentScopeAuthorization.requireScope("write:ai_agent"))
        .requestMatchers("/api/mcp/**", "/api/tools/**", "/api/eval/**")
        .access(JwtPresentScopeAuthorization.requireScope("write:ai_tools"))
        .anyRequest()
        .permitAll();
  }

  private static void applyJwtResourceServer(
      HttpSecurity http,
      ObjectProvider<JwtDecoder> jwtDecoder,
      JwtAuthenticationConverter converter)
      throws Exception {
    if (jwtDecoder.getIfAvailable() != null) {
      http.oauth2ResourceServer(
          oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
    }
  }
}
