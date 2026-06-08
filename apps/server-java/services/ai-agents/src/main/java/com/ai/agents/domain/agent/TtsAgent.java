package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TTS (Text-to-Speech) agent for synthesizing speech.
 */
@Component
public class TtsAgent extends AbstractAiAgent {

    private static final String SYSTEM_PROMPT = """
            You are a TTS (Text-to-Speech) assistant that helps synthesize speech from text.

            You don't generate actual audio, but you help format text for TTS processing.
            """;

    public TtsAgent() {
        super("tts", "TTS Agent", "Text-to-Speech synthesis", AgentType.TTS);
    }

    @Override
    public boolean canHandle(AgentRequest request) {
        String msg = request.message().toLowerCase();
        return msg.contains("tts") || msg.contains("speech") ||
               msg.contains("voice") || msg.contains("audio") ||
               msg.contains("synthesize") || msg.contains("text to speech");
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        String text = request.message();

        return """
                TTS Request received.

                Text to synthesize: "%s"

                To use TTS, call the TTS service endpoint:
                POST /api/tts/synthesize

                With body: { "text": "%s", "voiceId": "default" }
                """.formatted(text, text);
    }
}
