package com.ai.account.controller.dto;

import java.util.List;

/** Current viewer identity: anonymous guest (Client Identity) or authenticated OAuth user. */
public record AccountMeResponse(
    String mode,
    String clientId,
    String userId,
    String email,
    String plan,
    boolean loginAvailable,
    List<String> loginProviders) {}
