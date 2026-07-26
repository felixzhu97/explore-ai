package com.ai.metrics.application;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("AiInvocationRecorder")
class AiInvocationRecorderTest {

    @Test
    @DisplayName("should_persist_event_when_repository_succeeds")
    void should_persist_event_when_repository_succeeds() {
        List<AiInvocationEvent> saved = new ArrayList<>();
        AiInvocationRecorder recorder = new AiInvocationRecorder(
                new AiInvocationEventRepository() {
                    @Override
                    public void save(AiInvocationEvent event) {
                        saved.add(event);
                    }

                    @Override
                    public PageResult findDrilldown(DrilldownQuery query) {
                        return new PageResult(List.of(), 0);
                    }
                },
                new SimpleMeterRegistry());

        recorder.recordSuccess(AiDomain.CHAT, "chat.stream", 15, "openai", "gpt", "s1");

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getOutcome()).isEqualTo(InvocationOutcome.SUCCESS);
    }

    @Test
    @DisplayName("should_not_throw_when_repository_fails")
    void should_not_throw_when_repository_fails() {
        AiInvocationRecorder recorder = new AiInvocationRecorder(
                new AiInvocationEventRepository() {
                    @Override
                    public void save(AiInvocationEvent event) {
                        throw new IllegalStateException("db down");
                    }

                    @Override
                    public PageResult findDrilldown(DrilldownQuery query) {
                        return new PageResult(List.of(), 0);
                    }
                },
                new SimpleMeterRegistry());

        assertThatCode(() -> recorder.recordError(
                AiDomain.RAG, "rag.chat", 20, "openai", null, null, "ERR", "boom"))
                .doesNotThrowAnyException();
    }
}
