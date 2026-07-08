package com.ai.audio.application.usecase;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechRepository;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AudioFacade {

    private static final Logger log = LoggerFactory.getLogger(AudioFacade.class);

    private final TextToSpeechRepository textToSpeechRepository;

    public AudioFacade(TextToSpeechRepository textToSpeechRepository) {
        this.textToSpeechRepository = textToSpeechRepository;
    }

    public byte[] synthesize(String text) {
        log.info("AudioFacade.synthesize: {}", truncate(text));
        SynthesizedAudio audio = textToSpeechRepository.synthesize(SpeechText.of(text));
        return audio.isEmpty() ? null : audio.data();
    }

    public List<String> getAvailableVoices() {
        return VoiceCatalog.defaults().voices();
    }

    public List<String> getAvailableTtsModels() {
        return VoiceCatalog.defaults().models();
    }

    private String truncate(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= 50) {
            return text;
        }
        return text.substring(0, 50) + "...";
    }
}
