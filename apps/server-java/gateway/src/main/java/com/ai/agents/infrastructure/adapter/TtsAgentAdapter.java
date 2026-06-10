package com.ai.agents.infrastructure.adapter;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import com.ai.tts.application.service.TtsApplicationService;
import com.ai.tts.domain.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

/**
 * TTS Agent Adapter.
 * Delegates to internal TtsApplicationService instead of external WebClient call.
 */
@Component
public class TtsAgentAdapter implements AgentAdapter {

    private static final Logger log = LoggerFactory.getLogger(TtsAgentAdapter.class);

    private final TtsApplicationService ttsService;

    public TtsAgentAdapter(TtsApplicationService ttsService) {
        this.ttsService = ttsService;
    }

    @Override
    public AgentType getType() {
        return AgentType.TTS;
    }

    @Override
    public Mono<AgentResponseDto> execute(Conversation conversation, AgentRequestDto request) {
        log.info("TTS agent processing request: {}", truncate(request.getUserMessage(), 50));

        Map<String, Object> metadata = request.metadata();
        String text = request.getUserMessage();
        String voice = metadata != null ? (String) metadata.get("voice") : null;
        String language = metadata != null ? (String) metadata.get("language") : null;
        Float speed = metadata != null && metadata.get("speed") instanceof Number n ? n.floatValue() : 1.0f;

        return ttsService.synthesize(
                        text,
                        voice,
                        language,
                        speed,
                        0f,
                        OutputFormat.MP3,
                        null
                )
                .map(audioResult -> {
                    String audioBase64 = Base64.getEncoder().encodeToString(audioResult.audioData());
                    Map<String, Object> responseMetadata = Map.of(
                            "audioLength", audioResult.sizeInBytes(),
                            "audioData", audioBase64,
                            "format", audioResult.format().name()
                    );
                    return AgentResponseDto.success(
                                    "TTS synthesized successfully",
                                    AgentType.TTS
                            )
                            .withMetadata(responseMetadata);
                })
                .onErrorResume(e -> {
                    log.error("TTS processing failed", e);
                    return Mono.just(AgentResponseDto.error("TTS failed: " + e.getMessage()));
                });
    }

    @Override
    public boolean isAvailable() {
        return ttsService != null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
