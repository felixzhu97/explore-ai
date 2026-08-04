package com.ai.tools.domain;

public class InvalidWeatherQueryException extends RuntimeException {

    public InvalidWeatherQueryException(String message) {
        super(message);
    }
}
