package com.ai.workflow.domain;

/**
 * Classification decision and specialized-route output from a routing workflow.
 */
public record RoutingResult(String selection, String reasoning, String output) {
}
