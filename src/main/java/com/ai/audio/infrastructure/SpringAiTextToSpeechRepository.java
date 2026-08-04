package com.ai.audio.infrastructure;

import com.ai.audio.domain.SynthesizedAudio;
import com.ai.audio.domain.TextToSpeechRepository;
import com.ai.audio.domain.SpeechText;
import com.ai.audio.domain.VoiceSelection;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SpringAiTextToSpeechRepository implements TextToSpeechRepository {

    private final TextToSpeechModel textToSpeechModel;

    public SpringAiTextToSpeechRepository(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    @Override
    public SynthesizedAudio synthesize(SpeechText text, VoiceSelection voiceSelection, Double speed) {
        OpenAiAudioSpeechOptions.Builder optionsBuilder = OpenAiAudioSpeechOptions.builder()
                .voice(voiceSelection.voice());
        if (StringUtils.hasText(voiceSelection.model())) {
            optionsBuilder.model(voiceSelection.model());
        }
        if (speed != null) {
            optionsBuilder.speed(speed);
        }

        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text.value(), optionsBuilder.build());
        TextToSpeechResponse response = textToSpeechModel.call(prompt);

        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            byte[] audio = response.getResults().getFirst().getOutput();
            return SynthesizedAudio.create(audio);
        }
        return SynthesizedAudio.empty();
    }
}
