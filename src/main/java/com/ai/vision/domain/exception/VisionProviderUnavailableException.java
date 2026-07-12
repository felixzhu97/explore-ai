package com.ai.vision.domain.exception;

public class VisionProviderUnavailableException extends RuntimeException {

    private final String provider;

    public VisionProviderUnavailableException(String provider, String message) {
        super(message);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
