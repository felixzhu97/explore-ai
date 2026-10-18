package com.ai.scim.service;

import com.ai.account.domain.model.ContactEmail;

/** Desired account state read from a SCIM User resource. */
public record ScimAccountProfile(
    String externalId, String userName, ContactEmail email, String displayName, boolean active) {}
