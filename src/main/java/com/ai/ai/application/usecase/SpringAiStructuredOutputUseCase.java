package com.ai.ai.application.usecase;

import com.ai.ai.web.dto.TextAnalysisResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.stereotype.Service;

/**
 * Spring AI implementation of structured output use case using .entity() method.
 */
@Service
public class SpringAiStructuredOutputUseCase implements StructuredOutputUseCasePort {

    private final ChatClient chatClient;

    private static final String ANALYSIS_PROMPT = """
            Analyze the following text and provide a structured response.
            
            Text: {text}
            
            Respond with a JSON object containing:
            - summary: A brief summary of the text (max 50 words)
            - sentiment: One of POSITIVE, NEUTRAL, or NEGATIVE
            - key_points: 3-5 key takeaways from the text
            - entities: List of named entities (people, places, organizations) mentioned
            - language: The detected language of the text
            
            Be concise and accurate in your analysis.
            """;

    public SpringAiStructuredOutputUseCase(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Analyzes text and returns structured result using Spring AI 2.0 structured output.
     * Uses .entity() method with native structured output enabled.
     *
     * @param text The text to analyze
     * @return Structured analysis result
     */
    @Override
    public TextAnalysisResult analyzeText(String text) {
        String prompt = ANALYSIS_PROMPT.replace("{text}", text);

        return chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(prompt)
                .call()
                .entity(TextAnalysisResult.class);
    }

    /**
     * Analyzes text with specified language context.
     *
     * @param text The text to analyze
     * @param language The language of the text
     * @return Structured analysis result
     */
    @Override
    public TextAnalysisResult analyzeTextWithLanguage(String text, String language) {
        String prompt = ANALYSIS_PROMPT.replace("{text}", text)
                + "\n\nPlease respond in " + (language != null ? language : "English") + ".";
        return chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(prompt)
                .call()
                .entity(TextAnalysisResult.class);
    }
}
