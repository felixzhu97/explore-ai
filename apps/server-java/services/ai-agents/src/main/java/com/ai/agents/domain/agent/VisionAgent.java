package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Vision agent for image analysis and generation.
 */
@Component
public class VisionAgent extends AbstractAiAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Vision AI assistant specialized in image analysis and generation.

            You can help with:
            - Image description and captioning
            - Object detection
            - Image classification
            - Generating image prompts
            """;

    public VisionAgent() {
        super("vision", "Vision Agent", "Image analysis and generation", AgentType.VISION);
    }

    @Override
    public boolean canHandle(AgentRequest request) {
        String msg = request.message().toLowerCase();
        return msg.contains("image") || msg.contains("picture") ||
               msg.contains("photo") || msg.contains("vision") ||
               msg.contains("detect") || msg.contains("recognize");
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        return """
                Vision Request received.

                To use Vision capabilities, call the Vision service endpoint:
                POST /api/vision/analyze (for image analysis)
                POST /api/vision/generate (for image generation)
                """;
    }
}
