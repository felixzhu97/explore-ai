package com.ai.workflow.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Workflow request validation")
class WorkflowRequestValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  @DisplayName("should reject parallel request when parallelism exceeds the cap")
  void shouldRejectParallelRequestWhenParallelismExceedsTheCap() {
    var request = new ParallelizationWorkflowRequest("Summarize", List.of("a"), 1_000);

    assertThat(violatedFields(validator.validate(request))).containsExactly("parallelism");
  }

  @Test
  @DisplayName("should reject parallel request when too many items are sent")
  void shouldRejectParallelRequestWhenTooManyItemsAreSent() {
    var items = Collections.nCopies(WorkflowLimits.MAX_ITEMS + 1, "item");
    var request = new ParallelizationWorkflowRequest("Summarize", items, 2);

    assertThat(violatedFields(validator.validate(request))).containsExactly("items");
  }

  @Test
  @DisplayName("should reject chain request when too many steps are sent")
  void shouldRejectChainRequestWhenTooManyStepsAreSent() {
    var prompts = Collections.nCopies(WorkflowLimits.MAX_CHAIN_STEPS + 1, "step");
    var request = new ChainWorkflowRequest("input", prompts);

    assertThat(violatedFields(validator.validate(request))).containsExactly("systemPrompts");
  }

  @Test
  @DisplayName("should accept request when it is within limits")
  void shouldAcceptRequestWhenItIsWithinLimits() {
    var request = new ParallelizationWorkflowRequest("Summarize", List.of("a", "b"), 8);

    assertThat(validator.validate(request)).isEmpty();
  }

  private static <T> List<String> violatedFields(Set<ConstraintViolation<T>> violations) {
    return violations.stream().map(v -> v.getPropertyPath().toString()).distinct().toList();
  }
}
