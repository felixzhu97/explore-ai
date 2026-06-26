package com.ai.ai.domain.vo;

public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant");
    
    private final String value;
    
    MessageRole(String value) {
        this.value = value;
    }
    
    public String value() {
        return value;
    }
    
    public static MessageRole from(String value) {
        if (value == null || value.isBlank()) return USER;
        String normalized = value.toLowerCase().trim();
        for (MessageRole role : values()) {
            if (role.value.equals(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Role must be either 'user' or 'assistant', got: " + value);
    }
}
