package com.ai.account.application.usecase;

import com.ai.account.domain.model.AccountUser;
import com.ai.account.domain.repository.AccountUserRepository;
import com.ai.account.infrastructure.config.OAuthGoogleProperties;
import com.ai.account.web.dto.AccountMeResponse;
import com.ai.billing.infrastructure.config.BillingProperties;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@EnableConfigurationProperties({BillingProperties.class, OAuthGoogleProperties.class})
public class AccountUseCaseImpl implements AccountUseCase {

    private final AccountUserRepository accountUserRepository;
    private final BillingProperties billingProperties;
    private final OAuthGoogleProperties oauthGoogleProperties;

    public AccountUseCaseImpl(
            AccountUserRepository accountUserRepository,
            BillingProperties billingProperties,
            OAuthGoogleProperties oauthGoogleProperties) {
        this.accountUserRepository = accountUserRepository;
        this.billingProperties = billingProperties;
        this.oauthGoogleProperties = oauthGoogleProperties;
    }

    @Override
    public AccountMeResponse currentAccount(String clientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(authentication)) {
            OAuthIdentity identity = extractIdentity(authentication);
            if (identity != null) {
                Optional<AccountUser> linked =
                        accountUserRepository.findByProviderAndSubject(identity.provider(), identity.subject());
                String email = linked.map(AccountUser::getEmail).orElse(identity.email());
                String userId = linked.map(AccountUser::getId).orElse(identity.subject());
                return authenticated(clientId, userId, email);
            }
        }

        // Session may be missing after a host mismatch; Client Identity link still proves login.
        Optional<AccountUser> byClient = accountUserRepository.findByLinkedClientId(clientId);
        if (byClient.isPresent()) {
            AccountUser user = byClient.get();
            return authenticated(clientId, user.getId(), user.getEmail());
        }

        return new AccountMeResponse(
                "anonymous",
                clientId,
                null,
                null,
                billingProperties.getPlan(),
                isLoginAvailable());
    }

    @Override
    @Transactional
    public void linkOAuthUser(String provider, String subject, String email, String clientId) {
        AccountUser user = accountUserRepository
                .findByProviderAndSubject(provider, subject)
                .orElseGet(() -> AccountUser.create(provider, subject, email, clientId));
        user.linkSession(email, clientId);
        accountUserRepository.save(user);
    }

    @Override
    @Transactional
    public void unlinkClient(String clientId) {
        accountUserRepository.findByLinkedClientId(clientId).ifPresent(user -> {
            user.unlinkBrowser();
            accountUserRepository.save(user);
        });
    }

    @Override
    public boolean isLoginAvailable() {
        return oauthGoogleProperties.isEnabled()
                && oauthGoogleProperties.getClientId() != null
                && !oauthGoogleProperties.getClientId().isBlank()
                && oauthGoogleProperties.getClientSecret() != null
                && !oauthGoogleProperties.getClientSecret().isBlank();
    }

    private AccountMeResponse authenticated(String clientId, String userId, String email) {
        return new AccountMeResponse(
                "authenticated",
                clientId,
                userId,
                email,
                billingProperties.getPlan(),
                isLoginAvailable());
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static OAuthIdentity extractIdentity(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            String subject = oidcUser.getSubject();
            if (subject == null || subject.isBlank()) {
                return null;
            }
            String email = oidcUser.getEmail();
            if (email == null || email.isBlank()) {
                email = oidcUser.getAttribute("email");
            }
            return new OAuthIdentity("google", subject, email);
        }
        if (principal instanceof OAuth2User oauth2User) {
            String subject = oauth2User.getName();
            if (subject == null || subject.isBlank()) {
                return null;
            }
            String email = oauth2User.getAttribute("email");
            return new OAuthIdentity("google", subject, email);
        }
        return null;
    }

    private record OAuthIdentity(String provider, String subject, String email) {}
}
