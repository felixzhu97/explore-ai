package com.ai.domain.service;

import com.ai.adapter.in.dto.TextAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.AdvisorParams;

/**
 * Domain service for structured output using Spring AI 2.0 .entity() method.
 */
public class StructuredOutputService {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputService.class);

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

    public StructuredOutputService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Analyzes text and returns structured result using Spring AI 2.0 structured output.
     * Uses .entity() method with native structured output enabled.
     *
     * @param text The text to analyze
     * @return Structured analysis result
     */
    public TextAnalysisResult analyzeText(String text) {
        log.info("Analyzing text of length: {}", text != null ? text.length() : 0);

        String prompt = ANALYSIS_PROMPT.replace("{text}", text);

        TextAnalysisResult result = chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(prompt)
                .call()
                .entity(TextAnalysisResult.class);

        log.info("Analysis completed: sentiment={}, keyPoints={}", 
                result.sentiment(), result.keyPoints() != null ? result.keyPoints().size() : 0);

        return result;
    }

    /**
     * Analyzes text with specified language context.
     *
     * @param text The text to analyze
     * @param language The language of the text
     * @return Structured analysis result
     */
    public TextAnalysisResult analyzeTextWithLanguage(String text, String language) {
        log.info("Analyzing text in language: {}", language);

        String prompt = ANALYSIS_PROMPT.replace("{text}", text);

        TextAnalysisResult result = chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(prompt)
                .call()
                .entity(TextAnalysisResult.class);

        return result;
    }
}
