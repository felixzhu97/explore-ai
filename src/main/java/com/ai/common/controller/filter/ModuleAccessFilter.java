package com.ai.common.controller.filter;

import com.ai.common.domain.vo.ModuleFlag;
import com.ai.common.service.featureflag.FeatureFlagService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Documentation. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ModuleAccessFilter extends OncePerRequestFilter {

  private final FeatureFlagService featureFlagService;

  /** Documentation. */
  public ModuleAccessFilter(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    if (request.getPathInfo() != null) {
      path += request.getPathInfo();
    }
    ModuleFlag module = ModuleFlag.fromPath(path);
    if (module != null && !featureFlagService.isModuleEnabled(module)) {
      response.sendError(HttpStatus.NOT_FOUND.value());
      return;
    }
    filterChain.doFilter(request, response);
  }
}
