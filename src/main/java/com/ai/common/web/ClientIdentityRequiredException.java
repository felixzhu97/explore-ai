package com.ai.common.web;

/**
 * Raised when an anonymous client identity cookie is missing from the request context.
 */
public class ClientIdentityRequiredException extends RuntimeException {

    public ClientIdentityRequiredException() {
        super("Client identity is required");
    }
}
