package com.ai.a2a.controller.dto;

import com.ai.a2a.domain.model.AgentCard;
import java.util.List;
import java.util.Map;

/** HTTP JSON for {@code /.well-known/agent-card.json}. */
public record AgentCardResponse(
    String name,
    String description,
    String version,
    String url,
    String protocolVersion,
    Capabilities capabilities,
    List<String> defaultInputModes,
    List<String> defaultOutputModes,
    List<Skill> skills,
    List<SupportedInterface> supportedInterfaces,
    Map<String, SecurityScheme> securitySchemes,
    List<Map<String, List<String>>> security) {

  /** Builds the public Agent Card response from the domain Card. */
  public static AgentCardResponse from(AgentCard card) {
    List<Skill> skills =
        card.getSkills().stream()
            .map(
                skill ->
                    new Skill(
                        skill.getId(),
                        skill.getName(),
                        skill.getDescription(),
                        List.of("explore-ai", skill.getId())))
            .toList();
    List<SupportedInterface> interfaces =
        card.getSupportedInterfaces().stream()
            .map(
                item ->
                    new SupportedInterface(
                        item.get("url"), item.get("protocolBinding"), item.get("protocolVersion")))
            .toList();
    return new AgentCardResponse(
        card.getName(),
        card.getDescription(),
        card.getVersion(),
        card.getUrl(),
        "1.0",
        new Capabilities(true, false),
        List.of("text/plain"),
        List.of("text/plain"),
        skills,
        interfaces,
        Map.of(
            "oauth2",
            new SecurityScheme(
                "oauth2", "Explore IAM OIDC; message APIs require write:ai_agent scope")),
        List.of(Map.of("oauth2", List.of("write:ai_agent"))));
  }

  /** A2A Card capability flags. */
  public record Capabilities(boolean streaming, boolean pushNotifications) {}

  /** One advertised skill. */
  public record Skill(String id, String name, String description, List<String> tags) {}

  /** One supported transport interface. */
  public record SupportedInterface(String url, String protocolBinding, String protocolVersion) {}

  /** OAuth2 (or other) security scheme entry. */
  public record SecurityScheme(String type, String description) {}
}
