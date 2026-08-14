package com.ai.pipeline.application;

import java.util.List;

/** Builtin agent template loaded from classpath JSON (one locale file per language). */
public record AgentTemplate(
    String id,
    String typeKey,
    String name,
    String description,
    String systemPrompt,
    List<String> toolKeys,
    String runtime) {}
