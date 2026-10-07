package com.ai.account.service;

import com.ai.account.domain.model.ContactEmail;
import com.ai.account.domain.model.ExternalIdentity;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Identity and profile read from a Google / GitHub / IAM browser OAuth session. */
public record OAuthSignIn(ExternalIdentity identity, ContactEmail email, String displayName) {

  public OAuthSignIn {
    Objects.requireNonNull(identity, "identity");
    displayName = displayName == null || displayName.isBlank() ? null : displayName.trim();
  }

  /** Reads the sign-in from an OAuth session, or empty for guests and unknown principals. */
  public static Optional<OAuthSignIn> createSignIn(Authentication authentication) {
    if (!(authentication instanceof OAuth2AuthenticationToken token)
        || !token.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return Optional.empty();
    }
    String provider = token.getAuthorizedClientRegistrationId();
    if (token.getPrincipal() instanceof OidcUser oidcUser) {
      return of(provider, oidcUser.getSubject(), oidcEmail(oidcUser), null);
    }
    if (token.getPrincipal() instanceof OAuth2User oauth2User) {
      // GitHub hides private addresses; its login is a handle, so it goes to displayName.
      return of(
          provider,
          oauth2User.getName(),
          oauth2User.getAttribute("email"),
          oauth2User.getAttribute("login"));
    }
    return Optional.empty();
  }

  /** Returns the name to show before an account exists: display name first, then email. */
  public Optional<String> displayLabel() {
    if (displayName != null) {
      return Optional.of(displayName);
    }
    return Optional.ofNullable(email).map(ContactEmail::getValue);
  }

  private static Optional<OAuthSignIn> of(
      String provider, String subject, String email, String displayName) {
    if (provider == null || provider.isBlank() || subject == null || subject.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        new OAuthSignIn(
            ExternalIdentity.createIdentity(provider, subject),
            ContactEmail.parseOptionalEmail(email),
            displayName));
  }

  private static String oidcEmail(OidcUser oidcUser) {
    String email = oidcUser.getEmail();
    return email == null || email.isBlank() ? oidcUser.getAttribute("email") : email;
  }
}
