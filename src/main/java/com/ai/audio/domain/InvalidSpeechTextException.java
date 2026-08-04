package com.ai.audio.domain;

public class InvalidSpeechTextException extends RuntimeException {

    public InvalidSpeechTextException(String message) {
        super(message);
    }
}
