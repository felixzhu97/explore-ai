package com.ai.account.web;

import com.ai.account.application.CurrentOwnerResolver;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.web.ClientIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Web helper: cookie Client Identity + security context → data {@link OwnerKey}.
 */
@Component
public class OwnerContext {

    private final CurrentOwnerResolver currentOwnerResolver;

    public OwnerContext(CurrentOwnerResolver currentOwnerResolver) {
        this.currentOwnerResolver = currentOwnerResolver;
    }

    public OwnerKey require(HttpServletRequest request) {
        String clientId = ClientIdentity.require(request);
        return currentOwnerResolver.resolve(
                clientId, SecurityContextHolder.getContext().getAuthentication());
    }

    public String requireValue(HttpServletRequest request) {
        return require(request).value();
    }
}
