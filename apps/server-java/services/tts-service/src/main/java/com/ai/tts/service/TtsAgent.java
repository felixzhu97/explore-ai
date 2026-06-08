package com.ai.tts.service;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TtsAgent implements Agent {

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
        return Mono.just(AgentResponse.success(
                "TTS request processed: " + request.message(),
                type()
        ));
    }
}
