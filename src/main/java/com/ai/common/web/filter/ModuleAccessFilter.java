package com.ai.common.web.filter;

import com.ai.common.application.featureflag.FeatureFlagService;
import com.ai.common.domain.vo.ModuleFlag;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ModuleAccessFilter extends OncePerRequestFilter {

    private final FeatureFlagService featureFlagService;

    public ModuleAccessFilter(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        ModuleFlag module = ModuleFlag.fromPath(request.getRequestURI());
        if (module != null && !featureFlagService.isModuleEnabled(module)) {
            response.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
