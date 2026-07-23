package com.ai.workflow.domain.service;

import com.ai.workflow.domain.model.RoutingResult;

import java.util.Map;

/**
 * Classify input then process with a specialized route prompt.
 */
public interface RoutingWorkflow {

    /**
     * @param input  content to classify and process
     * @param routes route name → specialized system/user prompt
     */
    RoutingResult route(String input, Map<String, String> routes);
}
