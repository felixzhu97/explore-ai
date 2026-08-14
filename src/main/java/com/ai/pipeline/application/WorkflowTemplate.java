package com.ai.pipeline.application;

import java.util.List;

/** Documentation. */
public record WorkflowTemplate(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String shortTopic,
    String briefPrompt) {}
