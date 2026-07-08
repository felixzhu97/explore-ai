package com.ai.audio.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * ASR transcription thread pool configuration.
 */
@Configuration
public class AsrConfig {

    @Bean(name = "asrTranscriptionExecutor")
    TaskExecutor asrTranscriptionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("asr-transcription-");
        executor.initialize();
        return executor;
    }
}
