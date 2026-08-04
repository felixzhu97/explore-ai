package com.ai.audio.application;

import com.ai.audio.domain.TtsProviderNotConfiguredException;
import com.ai.audio.domain.SynthesizedAudio;
import com.ai.audio.domain.TextToSpeechRepository;
import com.ai.audio.domain.SpeechText;
import com.ai.audio.domain.VoiceCatalog;
import com.ai.audio.domain.VoiceInfo;
import com.ai.audio.domain.VoiceSelection;
import com.ai.audio.domain.TtsConfiguration;
import com.ai.common.util.LogSanitizer;
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
        log.info("AudioFacade.synthesize: {}", LogSanitizer.truncate(text));
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

}
