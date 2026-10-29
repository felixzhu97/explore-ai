package com.ai.a2a.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/** A2A Agent Card: discovery manifest for Explore AI Agents. */
@Getter
public final class AgentCard {

  private final String name;
  private final String description;
  private final String version;
  private final String url;
  private final List<A2aSkill> skills;
  private final List<Map<String, String>> supportedInterfaces;

  private AgentCard(
      String name,
      String description,
      String version,
      String url,
      List<A2aSkill> skills,
      List<Map<String, String>> supportedInterfaces) {
    this.name = Objects.requireNonNull(name, "name");
    this.description = description == null ? "" : description;
    this.version = version == null ? "1.0.0" : version;
    this.url = Objects.requireNonNull(url, "url");
    this.skills = List.copyOf(skills);
    this.supportedInterfaces = List.copyOf(supportedInterfaces);
  }

  /** Builds a Card for the given public base URL and catalog skills. */
  public static AgentCard createCard(String baseUrl, List<A2aSkill> skills) {
    String root = trimTrailingSlash(baseUrl);
    String a2aUrl = root + "/a2a/v1";
    List<Map<String, String>> interfaces = new ArrayList<>();
    interfaces.add(Map.of("url", a2aUrl, "protocolBinding", "HTTP+JSON", "protocolVersion", "1.0"));
    return new AgentCard(
        "Explore AI",
        "Baseline Agents for sibling Explore products (reason, tools, reply).",
        "1.0.0",
        a2aUrl,
        skills == null ? List.of() : skills,
        interfaces);
  }

  /** Wire JSON for {@code /.well-known/agent-card.json}. */
  public Map<String, Object> toWireMap() {
    Map<String, Object> capabilities = new LinkedHashMap<>();
    capabilities.put("streaming", true);
    capabilities.put("pushNotifications", false);

    List<Map<String, Object>> skillMaps = new ArrayList<>();
    for (A2aSkill skill : skills) {
      Map<String, Object> s = new LinkedHashMap<>();
      s.put("id", skill.getId());
      s.put("name", skill.getName());
      s.put("description", skill.getDescription());
      s.put("tags", List.of("explore-ai", skill.getId()));
      skillMaps.add(s);
    }

    Map<String, Object> securitySchemes = new LinkedHashMap<>();
    securitySchemes.put(
        "oauth2",
        Map.of(
            "type",
            "oauth2",
            "description",
            "Explore IAM OIDC; message APIs require write:ai_agent scope"));

    Map<String, Object> card = new LinkedHashMap<>();
    card.put("name", name);
    card.put("description", description);
    card.put("version", version);
    card.put("url", url);
    card.put("protocolVersion", "1.0");
    card.put("capabilities", capabilities);
    card.put("defaultInputModes", List.of("text/plain"));
    card.put("defaultOutputModes", List.of("text/plain"));
    card.put("skills", skillMaps);
    card.put("supportedInterfaces", supportedInterfaces);
    card.put("securitySchemes", securitySchemes);
    card.put("security", List.of(Map.of("oauth2", List.of("write:ai_agent"))));
    return card;
  }

  private static String trimTrailingSlash(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "http://localhost:9000";
    }
    String trimmed = baseUrl.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
