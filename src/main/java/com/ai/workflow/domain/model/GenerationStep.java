package com.ai.workflow.domain.model;

import lombok.Value;

/** One generator iteration in the evaluator-optimizer loop. */
@Value
public class GenerationStep {
  String thoughts;
  String response;
}
