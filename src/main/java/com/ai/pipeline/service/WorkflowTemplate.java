package com.ai.pipeline.service;

import java.util.List;

/** Documentation. */
public record WorkflowTemplate(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String shortTopic,
    String briefPrompt) {}
