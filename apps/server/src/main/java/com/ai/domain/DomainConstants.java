package com.ai.domain;

/**
 * Domain layer common constants.
 */
public final class DomainConstants {

    private DomainConstants() {
        // Prevent instantiation
    }

    public static final String DEFAULT_SESSION_TITLE = "New Chat";
    public static final int MAX_MESSAGE_LENGTH = 10000;
    public static final int DEFAULT_RECENT_MESSAGES_COUNT = 10;
}
