package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class TtsAgent implements Agent {

    private final WebClient webClient;
    private final String ttsServiceUrl;

    public TtsAgent(
            WebClient.Builder webClientBuilder,
            @Value("${ai.agent.services.tts.base-url}") String ttsServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(ttsServiceUrl).build();
        this.ttsServiceUrl = ttsServiceUrl;
    }

    @Override
    public String name() {
        return "TtsAgent";
    }

    @Override
    public AgentType type() {
        return AgentType.TTS;
    }

    @Override
    public Mono<AgentResponse> process(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        String voice = metadata != null ? (String) metadata.get("voice") : null;
        String language = metadata != null ? (String) metadata.get("language") : null;
        Double speed = metadata != null && metadata.get("speed") instanceof Number n ? n.doubleValue() : 1.0;

        Map<String, Object> body = Map.of(
            "text", request.message(),
            "voice", voice != null ? voice : "",
            "language", language != null ? language : "",
            "speed", speed
        );

        return webClient.post()
                .uri("/tts/synthesize")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(byte[].class)
                .map(audio -> {
                    String audioBase64 = java.util.Base64.getEncoder().encodeToString(audio);
                    Map<String, Object> responseMetadata = Map.of(
                        "audioLength", audio.length,
                        "audioData", audioBase64
                    );
                    return AgentResponse.success(
                            "TTS synthesized successfully",
                            type()
                    ).withMetadata(responseMetadata);
                })
                .onErrorResume(e -> {
                    return Mono.just(AgentResponse.error("TTS failed: " + e.getMessage()));
                });
    }
}
