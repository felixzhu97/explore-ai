package com.ai.a2a.controller;

import com.ai.a2a.service.A2aService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public A2A Agent Card discovery. */
@RestController
@RequiredArgsConstructor
public class AgentCardController {

  private final A2aService a2aService;

  /** Returns the Agent Card at the well-known path. */
  @GetMapping("/.well-known/agent-card.json")
  public ResponseEntity<Map<String, Object>> getAgentCard(HttpServletRequest request) {
    return ResponseEntity.ok(a2aService.buildAgentCard(publicBaseUrl(request)).toWireMap());
  }

  private static String publicBaseUrl(HttpServletRequest request) {
    String forwardedProto = request.getHeader("X-Forwarded-Proto");
    String forwardedHost = request.getHeader("X-Forwarded-Host");
    if (forwardedProto != null
        && !forwardedProto.isBlank()
        && forwardedHost != null
        && !forwardedHost.isBlank()) {
      return forwardedProto.split(",")[0].trim() + "://" + forwardedHost.split(",")[0].trim();
    }
    StringBuffer url = request.getRequestURL();
    String uri = request.getRequestURI();
    return url.substring(0, url.length() - uri.length());
  }
}
