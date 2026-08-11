package com.ai.plugin.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PluginNotFoundException extends RuntimeException {

    public PluginNotFoundException(String message) {
        super(message);
    }
}
