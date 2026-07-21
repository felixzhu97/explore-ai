package com.ai.workflow.domain.model;

/**
 * One generator iteration in the evaluator-optimizer loop.
 */
public record GenerationStep(String thoughts, String response) {
}
