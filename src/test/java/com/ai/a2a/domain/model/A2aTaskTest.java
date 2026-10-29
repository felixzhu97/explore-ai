package com.ai.a2a.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A2aTask")
class A2aTaskTest {

  @Test
  @DisplayName("should move from submitted through working to completed with artifact")
  void shouldCompleteWithTextArtifact() {
    A2aTask task = A2aTask.createSubmitted("task-1");
    assertThat(task.getStatus()).isEqualTo(A2aTaskStatus.SUBMITTED);

    task.markWorking();
    task.completeWithText("done");

    assertThat(task.getStatus()).isEqualTo(A2aTaskStatus.COMPLETED);
    assertThat(task.toWireMap().get("id")).isEqualTo("task-1");
    assertThat(task.getArtifacts()).hasSize(1);
  }

  @Test
  @DisplayName("should mark failed with error message")
  void shouldFailWithMessage() {
    A2aTask task = A2aTask.createSubmitted("task-2");
    task.markWorking();
    task.failWithMessage("boom");

    assertThat(task.getStatus()).isEqualTo(A2aTaskStatus.FAILED);
    assertThat(task.getErrorMessage()).isEqualTo("boom");
  }
}
