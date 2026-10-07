package com.ai.pipeline.service;

import java.util.List;

/** Builtin agent template loaded from classpath JSON (one locale file per language). */
public record AgentTemplate(
    String id,
    String agentType,
    String name,
    String description,
    String systemPrompt,
    List<String> tools,
    String runtime) {}
