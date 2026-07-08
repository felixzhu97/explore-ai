package com.ai.audio.infrastructure.tts;

import com.ai.audio.domain.model.SynthesizedAudio;
import com.ai.audio.domain.repository.TextToSpeechRepository;
import com.ai.audio.domain.vo.SpeechText;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.stereotype.Repository;

@Repository
public class SpringAiTextToSpeechRepository implements TextToSpeechRepository {

    private final TextToSpeechModel textToSpeechModel;

    public SpringAiTextToSpeechRepository(TextToSpeechModel textToSpeechModel) {
        this.textToSpeechModel = textToSpeechModel;
    }

    @Override
    public SynthesizedAudio synthesize(SpeechText text) {
        TextToSpeechPrompt prompt = new TextToSpeechPrompt(text.value());
        TextToSpeechResponse response = textToSpeechModel.call(prompt);

        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            byte[] audio = response.getResults().getFirst().getOutput();
            return SynthesizedAudio.create(audio);
        }
        return SynthesizedAudio.empty();
    }
}
