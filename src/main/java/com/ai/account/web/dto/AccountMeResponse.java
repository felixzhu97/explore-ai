package com.ai.account.web.dto;

/**
 * Current viewer identity: anonymous guest (Client Identity) or authenticated OAuth user.
 */
public record AccountMeResponse(
        String mode,
        String clientId,
        String userId,
        String email,
        String plan,
        boolean loginAvailable
) {
}
