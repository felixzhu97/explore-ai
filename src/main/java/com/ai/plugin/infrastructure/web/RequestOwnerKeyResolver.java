package com.ai.plugin.infrastructure.web;

import com.ai.account.web.OwnerContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
public class RequestOwnerKeyResolver {

    private final OwnerContext ownerContext;

    public RequestOwnerKeyResolver(OwnerContext ownerContext) {
        this.ownerContext = ownerContext;
    }

    public Optional<String> currentOwnerKey() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return Optional.empty();
        }
        HttpServletRequest request = servletAttributes.getRequest();
        try {
            return Optional.of(ownerContext.requireValue(request));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
