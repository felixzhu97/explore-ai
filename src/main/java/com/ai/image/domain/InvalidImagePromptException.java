package com.ai.image.domain;

public class InvalidImagePromptException extends RuntimeException {

    public InvalidImagePromptException(String message) {
        super(message);
    }
}
