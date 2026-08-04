package com.ai.workflow.domain;

/**
 * One generator iteration in the evaluator-optimizer loop.
 */
public record GenerationStep(String thoughts, String response) {
}
