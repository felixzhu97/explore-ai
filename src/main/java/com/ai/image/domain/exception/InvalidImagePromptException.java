package com.ai.image.domain.exception;

public class InvalidImagePromptException extends RuntimeException {

    public InvalidImagePromptException(String message) {
        super(message);
    }
}
