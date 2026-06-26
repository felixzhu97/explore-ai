package com.ai.audio.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade for audio/speech synthesis operations.
 */
@Service
public class AudioFacade {

    private static final Logger log = LoggerFactory.getLogger(AudioFacade.class);

    private final TextToSpeechUseCase textToSpeechUseCase;

    public AudioFacade(TextToSpeechUseCase textToSpeechUseCase) {
        this.textToSpeechUseCase = textToSpeechUseCase;
    }

    /**
     * Synthesize text to speech.
     */
    public byte[] synthesize(String text) {
        log.info("AudioFacade.synthesize: {}", truncate(text));
        return textToSpeechUseCase.synthesize(text);
    }

    /**
     * Get available TTS voices.
     */
    public List<String> getAvailableVoices() {
        return textToSpeechUseCase.getAvailableVoices();
    }

    /**
     * Get available TTS models.
     */
    public List<String> getAvailableTtsModels() {
        return textToSpeechUseCase.getAvailableModels();
    }

    private String truncate(String text) {
        if (text == null) return "null";
        if (text.length() <= 50) return text;
        return text.substring(0, 50) + "...";
    }
}
