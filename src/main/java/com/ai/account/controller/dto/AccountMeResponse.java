package com.ai.account.controller.dto;

import java.util.List;

/** Current viewer identity: anonymous guest (Client Identity) or authenticated OAuth user. */
public record AccountMeResponse(
    AccountMode mode,
    String clientId,
    String userId,
    String email,
    AccountPlan plan,
    boolean loginAvailable,
    List<LoginProvider> loginProviders) {}
