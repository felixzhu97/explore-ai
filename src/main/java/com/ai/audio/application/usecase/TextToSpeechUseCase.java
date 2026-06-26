package com.ai.audio.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Domain service for text-to-speech using Spring AI 2.0 TextToSpeechModel API.
 */
@Service
public class TextToSpeechUseCase {

    private static final Logger log = LoggerFactory.getLogger(TextToSpeechUseCase.class);

    private final TextToSpeechModel textToSpeechModel;

    public TextToSpeechUseCase(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    /**
     * Synthesize text to speech.
     *
     * @param text The text to convert to speech
     * @return Audio bytes (MP3 format)
     */
    public byte[] synthesize(String text) {
        log.info("Synthesizing text to speech: {}", truncate(text, 50));

        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
        TextToSpeechResponse response = textToSpeechModel.call(prompt);

        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            byte[] audio = response.getResults().get(0).getOutput();
            log.info("Synthesized audio size: {} bytes", audio.length);
            return audio;
        }

        return null;
    }

    /**
     * Stream text to speech in real-time.
     *
     * @param text The text to convert to speech
     * @return Flux of audio bytes
     */
    public Flux<byte[]> stream(String text) {
        log.info("Streaming text to speech: {}", truncate(text, 50));

        return textToSpeechModel.stream(text);
    }

    /**
     * Get available voices.
     */
    public List<String> getAvailableVoices() {
        return List.of(
                "alloy",
                "echo",
                "fable",
                "onyx",
                "nova",
                "shimmer"
        );
    }

    /**
     * Get available models.
     */
    public List<String> getAvailableModels() {
        return List.of(
                "gpt-4o-mini-tts",
                "gpt-4o-tts",
                "tts-1",
                "tts-1-hd"
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
