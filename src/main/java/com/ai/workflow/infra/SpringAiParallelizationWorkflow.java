package com.ai.workflow.infra;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.workflow.domain.model.ParallelizationResult;
import com.ai.workflow.service.ParallelizationWorkflow;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Parallel sectioning/voting aligned with Spring AI agentic-patterns/parallelization-workflow. */
@Component
@RequiredArgsConstructor
public class SpringAiParallelizationWorkflow implements ParallelizationWorkflow {

  private static final int MAX_PARALLELISM = 8;
  private static final Duration TIMEOUT = Duration.ofMinutes(3);

  private final ChatClientProvider chatClientProvider;

  @Override
  public ParallelizationResult runParallel(String prompt, List<String> items, int parallelism) {
    Objects.requireNonNull(prompt, "prompt");
    Objects.requireNonNull(items, "items");
    if (items.isEmpty()) {
      throw new IllegalArgumentException("items must not be empty");
    }
    if (parallelism <= 0) {
      throw new IllegalArgumentException("parallelism must be greater than 0");
    }

    Semaphore permits = new Semaphore(Math.min(parallelism, MAX_PARALLELISM));
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    try {
      List<CompletableFuture<String>> futures =
          items.stream()
              .map(
                  item ->
                      CompletableFuture.supplyAsync(
                          () -> callWithPermit(permits, prompt, item), executor))
              .toList();

      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
          .orTimeout(TIMEOUT.toSeconds(), TimeUnit.SECONDS)
          .join();
      List<String> outputs = futures.stream().map(CompletableFuture::join).toList();
      return new ParallelizationResult(outputs);
    } finally {
      executor.shutdownNow();
    }
  }

  private String callWithPermit(Semaphore permits, String prompt, String item) {
    try {
      permits.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for a parallel slot", e);
    }
    try {
      String content =
          chatClientProvider
              .createBareStateless(TextChatOptions.defaults())
              .prompt()
              .user(prompt + "\nInput: " + item)
              .call()
              .content();
      return content == null ? "" : content;
    } catch (Exception e) {
      throw new RuntimeException("Failed to process a parallel input", e);
    } finally {
      permits.release();
    }
  }
}
