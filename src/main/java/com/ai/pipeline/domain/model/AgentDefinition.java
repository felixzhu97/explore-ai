package com.ai.pipeline.domain.model;

import java.util.List;
import java.util.Objects;
import lombok.Getter;

/** Immutable definition of a specialized or supervisor agent. */
@Getter
public final class AgentDefinition {

  public static final String RUNTIME_SINGLE = "single";
  public static final String RUNTIME_DEEP = "deep";

  private final AgentType type;
  private final String name;
  private final String description;
  private final String systemPrompt;
  private final List<String> tools;
  private final String runtime;
  private final boolean healthy;

  private AgentDefinition(
      AgentType type,
      String name,
      String description,
      String systemPrompt,
      List<String> tools,
      String runtime,
      boolean healthy) {
    this.type = Objects.requireNonNull(type, "type");
    this.name = Objects.requireNonNull(name, "name");
    this.description = Objects.requireNonNull(description, "description");
    this.systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt");
    this.tools = tools == null ? List.of() : List.copyOf(tools);
    this.runtime =
        runtime == null || runtime.isBlank() ? RUNTIME_SINGLE : runtime.trim().toLowerCase();
    this.healthy = healthy;
  }

  /** Creates a single-run agent without tools. */
  public static AgentDefinition createDefinition(
      AgentType type, String name, String description, String systemPrompt) {
    return createDefinition(type, name, description, systemPrompt, List.of(), RUNTIME_SINGLE);
  }

  /** Creates a healthy agent with tools and a runtime. */
  public static AgentDefinition createDefinition(
      AgentType type,
      String name,
      String description,
      String systemPrompt,
      List<String> tools,
      String runtime) {
    return new AgentDefinition(type, name, description, systemPrompt, tools, runtime, true);
  }

  /** Tells whether the agent can be a pipeline worker. */
  public boolean isWorker() {
    return !type.isSupervisor() && !isDeep();
  }

  /** Tells whether the agent uses the deep runtime. */
  public boolean isDeep() {
    return RUNTIME_DEEP.equals(runtime) || "deep".equals(type.getValue());
  }
}
