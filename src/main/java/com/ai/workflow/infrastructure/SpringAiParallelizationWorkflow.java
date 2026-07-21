package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.ParallelizationResult;
import com.ai.workflow.domain.service.ParallelizationWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Parallel sectioning/voting aligned with Spring AI agentic-patterns/parallelization-workflow.
 */
@Component
public class SpringAiParallelizationWorkflow implements ParallelizationWorkflow {

    private final ChatClientProvider chatClientProvider;

    public SpringAiParallelizationWorkflow(ChatClientProvider chatClientProvider) {
        this.chatClientProvider = Objects.requireNonNull(chatClientProvider);
    }

    @Override
    public ParallelizationResult parallel(String prompt, List<String> items, int parallelism) {
        Objects.requireNonNull(prompt, "prompt");
        Objects.requireNonNull(items, "items");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be greater than 0");
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            List<CompletableFuture<String>> futures = items.stream()
                    .map(item -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String content = chatClientProvider
                                    .createBareStateless(TextChatOptions.defaults())
                                    .prompt()
                                    .user(prompt + "\nInput: " + item)
                                    .call()
                                    .content();
                            return content == null ? "" : content;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to process input: " + item, e);
                        }
                    }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            List<String> outputs = futures.stream().map(CompletableFuture::join).toList();
            return new ParallelizationResult(outputs);
        } finally {
            executor.shutdown();
        }
    }
}
