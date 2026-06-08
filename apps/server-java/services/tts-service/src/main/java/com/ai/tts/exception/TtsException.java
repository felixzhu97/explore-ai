package com.ai.tts.exception;

public class TtsException extends RuntimeException {

    private final String errorCode;

    public TtsException(String message) {
        super(message);
        this.errorCode = "TTS_ERROR";
    }

    public TtsException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TtsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TTS_ERROR";
    }

    public TtsException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static TtsException providerNotFound(String provider) {
        return new TtsException(
            "TTS provider not found: " + provider,
            "PROVIDER_NOT_FOUND"
        );
    }

    public static TtsException synthesisFailed(Throwable cause) {
        return new TtsException(
            "Speech synthesis failed: " + cause.getMessage(),
            "SYNTHESIS_FAILED",
            cause
        );
    }

    public static TtsException invalidRequest(String message) {
        return new TtsException(message, "INVALID_REQUEST");
    }
}
