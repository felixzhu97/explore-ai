package com.ai.tools.domain.exception;

public class InvalidWeatherQueryException extends RuntimeException {

    public InvalidWeatherQueryException(String message) {
        super(message);
    }
}
