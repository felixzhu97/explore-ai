package com.ai.account.web;

/**
 * Current viewer identity. OAuth login will populate {@code userId} / {@code email} later.
 */
public record AccountMeResponse(
        String mode,
        String clientId,
        String userId,
        String email,
        String plan
) {
}
