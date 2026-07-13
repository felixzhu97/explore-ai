package com.ai.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class AiMetricsRecorder {

    private final Counter chatRequestCounter;
    private final Counter chatErrorCounter;
    private final Timer chatLatencyTimer;
    private final Counter ragRequestCounter;
    private final Counter ragRetrievalCounter;
    private final Timer ragLatencyTimer;
    private final Counter toolCallCounter;

    public AiMetricsRecorder(
            @Qualifier("chatRequestCounter") Counter chatRequestCounter,
            @Qualifier("chatErrorCounter") Counter chatErrorCounter,
            @Qualifier("chatLatencyTimer") Timer chatLatencyTimer,
            @Qualifier("ragRequestCounter") Counter ragRequestCounter,
            @Qualifier("ragRetrievalCounter") Counter ragRetrievalCounter,
            @Qualifier("ragLatencyTimer") Timer ragLatencyTimer,
            @Qualifier("toolCallCounter") Counter toolCallCounter) {
        this.chatRequestCounter = chatRequestCounter;
        this.chatErrorCounter = chatErrorCounter;
        this.chatLatencyTimer = chatLatencyTimer;
        this.ragRequestCounter = ragRequestCounter;
        this.ragRetrievalCounter = ragRetrievalCounter;
        this.ragLatencyTimer = ragLatencyTimer;
        this.toolCallCounter = toolCallCounter;
    }

    public <T> T recordChat(Supplier<T> action) {
        chatRequestCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            return action.get();
        } catch (RuntimeException exception) {
            chatErrorCounter.increment();
            throw exception;
        } finally {
            sample.stop(chatLatencyTimer);
        }
    }

    public void recordChatError() {
        chatErrorCounter.increment();
    }

    public void recordChatRequest() {
        chatRequestCounter.increment();
    }

    public <T> T recordRag(Supplier<T> action) {
        ragRequestCounter.increment();
        Timer.Sample sample = Timer.start();
        try {
            return action.get();
        } finally {
            sample.stop(ragLatencyTimer);
        }
    }

    public void recordRagRetrieval() {
        ragRetrievalCounter.increment();
    }

    public <T> T recordToolCall(Supplier<T> action) {
        toolCallCounter.increment();
        return action.get();
    }
}
