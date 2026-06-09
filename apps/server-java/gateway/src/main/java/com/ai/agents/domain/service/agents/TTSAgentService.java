package com.ai.agents.domain.service.agents;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * TTS Agent domain service.
 * Manages text-to-speech synthesis operations.
 */
@Service
public final class TTSAgentService {

    private final Map<String, VoiceProfile> voices = new HashMap<>();
    private final Map<String, SynthesisJob> jobs = new HashMap<>();

    public TTSAgentService() {
        initializeDefaultVoices();
    }

    private void initializeDefaultVoices() {
        voices.put("default", new VoiceProfile("default", "en-US", "female", "default"));
        voices.put("male", new VoiceProfile("male", "en-US", "male", "neural"));
        voices.put("female", new VoiceProfile("female", "en-US", "female", "neural"));
    }

    /**
     * Synthesize speech from text.
     */
    public SynthesisJob synthesize(String text, String voiceId, String outputFormat) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }

        String jobId = UUID.randomUUID().toString();
        SynthesisJob job = new SynthesisJob(
                jobId,
                text,
                voiceId != null ? voiceId : "default",
                outputFormat != null ? outputFormat : "mp3",
                "completed",
                null,
                Instant.now(),
                Instant.now()
        );

        jobs.put(jobId, job);
        return job;
    }

    /**
     * Get synthesis job.
     */
    public Optional<SynthesisJob> getJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    /**
     * List synthesis jobs.
     */
    public List<SynthesisJob> listJobs(String status) {
        return jobs.values().stream()
                .filter(j -> status == null || status.equals(j.status()))
                .toList();
    }

    /**
     * List available voices.
     */
    public List<VoiceProfile> listVoices() {
        return new ArrayList<>(voices.values());
    }

    /**
     * Get voice profile.
     */
    public Optional<VoiceProfile> getVoice(String voiceId) {
        return Optional.ofNullable(voices.get(voiceId));
    }

    public record VoiceProfile(
            String id,
            String language,
            String gender,
            String engine
    ) {}

    public record SynthesisJob(
            String jobId,
            String text,
            String voiceId,
            String outputFormat,
            String status,
            String audioUrl,
            Instant createdAt,
            Instant completedAt
    ) {
        public boolean isCompleted() { return "completed".equals(status); }
        public boolean isFailed() { return "failed".equals(status); }
    }
}
