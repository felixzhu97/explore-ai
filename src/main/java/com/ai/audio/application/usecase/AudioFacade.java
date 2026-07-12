package com.ai.audio.application.usecase;

import com.ai.audio.domain.exception.TtsProviderNotConfiguredException;
import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechRepository;
import com.ai.audio.domain.vo.SpeechText;
import com.ai.audio.domain.vo.VoiceCatalog;
import com.ai.audio.domain.vo.VoiceInfo;
import com.ai.audio.domain.vo.VoiceSelection;
import com.ai.audio.domain.repository.TtsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AudioFacade {

    private static final Logger log = LoggerFactory.getLogger(AudioFacade.class);

    private static final Map<String, String> LEGACY_VOICE_ALIASES = Map.of(
            "en-US", "alloy",
            "zh-CN", "alloy");

    private final TextToSpeechRepository textToSpeechRepository;
    private final TtsConfiguration ttsConfiguration;

    public AudioFacade(TextToSpeechRepository textToSpeechRepository, TtsConfiguration ttsConfiguration) {
        this.textToSpeechRepository = textToSpeechRepository;
        this.ttsConfiguration = ttsConfiguration;
    }

    public byte[] synthesize(String text, String voice, Double speed) {
        ensureProviderConfigured();
        log.info("AudioFacade.synthesize: {}", truncate(text));
        VoiceSelection selection = VoiceSelection.of(resolveVoice(voice), null);
        SynthesizedAudio audio =
                textToSpeechRepository.synthesize(SpeechText.of(text), selection, speed);
        return audio.isEmpty() ? null : audio.data();
    }

    public List<VoiceInfo> getAvailableVoices() {
        return VoiceCatalog.defaults().voiceInfos();
    }

    public List<String> getAvailableTtsModels() {
        return VoiceCatalog.defaults().models();
    }

    private void ensureProviderConfigured() {
        if (!ttsConfiguration.isEnabled()) {
            throw TtsProviderNotConfiguredException.disabled();
        }
        if (!ttsConfiguration.isConfigured()) {
            throw TtsProviderNotConfiguredException.apiKeyMissing();
        }
    }

    private String resolveVoice(String voice) {
        if (voice == null || voice.isBlank()) {
            return ttsConfiguration.getDefaultVoice();
        }
        String normalized = voice.trim();
        return LEGACY_VOICE_ALIASES.getOrDefault(normalized, normalized);
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
