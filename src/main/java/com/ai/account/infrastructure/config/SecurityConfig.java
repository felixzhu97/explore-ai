package com.ai.account.infrastructure.config;

import com.ai.account.infrastructure.oauth.AccountLogoutHandler;
import com.ai.account.infrastructure.oauth.OAuthLoginFailureHandler;
import com.ai.account.infrastructure.oauth.OAuthLoginSuccessHandler;
import com.ai.common.web.CsrfProtectionFilter;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Optional OAuth2 Login (Google and/or GitHub) on top of anonymous Client Identity (guest mode).
 *
 * <p>API routes stay permitAll. Spring Security CSRF is intentionally disabled for the SPA;
 * state-changing {@code /api/**} calls are protected by {@link CsrfProtectionFilter}
 * ({@code X-Requested-With: XMLHttpRequest}).
 *
 * @see CsrfProtectionFilter
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html">OAuth2 Login</a>
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html">OWASP CSRF</a>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(1)
    @ConditionalOnBean(ClientRegistrationRepository.class)
    SecurityFilterChain oauthSecurityFilterChain(
            HttpSecurity http,
            OAuthLoginSuccessHandler successHandler,
            OAuthLoginFailureHandler failureHandler,
            AccountLogoutHandler accountLogoutHandler,
            SecurityContextRepository securityContextRepository)
            throws Exception {
        http.securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // SPA uses CsrfProtectionFilter (custom header), not Spring CSRF tokens.
                // codeql[java/spring-disabled-csrf-protection]
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/account/logout")
                        .addLogoutHandler(accountLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    @Order(2)
    @ConditionalOnMissingBean(name = "oauthSecurityFilterChain")
    SecurityFilterChain guestOnlySecurityFilterChain(
            HttpSecurity http,
            AccountLogoutHandler accountLogoutHandler,
            SecurityContextRepository securityContextRepository)
            throws Exception {
        http.securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // SPA uses CsrfProtectionFilter (custom header), not Spring CSRF tokens.
                // codeql[java/spring-disabled-csrf-protection]
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .logout(logout -> logout
                        .logoutUrl("/api/account/logout")
                        .addLogoutHandler(accountLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"));
        return http.build();
    }
}
