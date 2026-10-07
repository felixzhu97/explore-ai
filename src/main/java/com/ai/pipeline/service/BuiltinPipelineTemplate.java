package com.ai.pipeline.service;

import java.util.List;

public record BuiltinPipelineTemplate(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String shortTopic,
    String briefPrompt) {}
