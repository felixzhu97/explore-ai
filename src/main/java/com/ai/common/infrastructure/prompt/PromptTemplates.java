package com.ai.common.infrastructure.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infrastructure service for centralized prompt templates.
 * Provides consistent prompt patterns across different use cases.
 */
public class PromptTemplates {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplates.class);

    private static final String MARKDOWN_FORMATTING_INSTRUCTIONS = """
            Format responses using GitHub Flavored Markdown (GFM):
            - Use ATX headings (# through ###) with one space after the hash marks
            - Put each heading and list on its own line; separate blocks with a blank line
            - Use unordered lists with "- " (hyphen followed by a space)
            - Use **bold** for emphasis within list items and paragraphs
            - Do not wrap the entire response in a code block unless asked

            Example:
            # Document Title

            ## 一、Section Heading (1511—1957)

            - **Label:** description text
            """;

    /**
     * A2UI v0.9 chart surfaces: NDJSON in ```a2ui fences (not raw ECharts option JSON).
     */
    private static final String A2UI_CHART_INSTRUCTIONS = """

            When a chart or visualization helps (trends, comparisons, distribution),
            emit an A2UI surface after a short markdown explanation using a fenced
            block labeled a2ui. Inside the fence, put one A2UI JSON message per line
            (NDJSON). Rules:
            - Set "version": "v0.9" on every message
            - First message: createSurface with catalogId
              "https://explore-ai.local/catalogs/chat-v0.9" and a surfaceId
            - Then updateComponents with a flat adjacency list that includes id "root"
              and a Chart component (type: bar|line|pie|doughnut; title; chartData as
              [{label,value}, ...] literal or data path)
            - Optional updateDataModel for bound chartData paths
            - Do NOT output executable JavaScript or bare ECharts option JSON
            - Do NOT invent other custom components beyond this catalog (incl. Chart)

            Example fence (abbreviated; real output must be valid NDJSON lines):
            ```a2ui
            {"version":"v0.9","createSurface":{"surfaceId":"s1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}
            {"version":"v0.9","updateComponents":{"surfaceId":"s1","components":[{"id":"root","component":"Column","children":["c1"]},{"id":"c1","component":"Chart","type":"bar","title":"Sales","chartData":[{"label":"Q1","value":10},{"label":"Q2","value":20}]}]}}
            ```
            """;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful AI assistant. Provide accurate and concise responses.

            """ + MARKDOWN_FORMATTING_INSTRUCTIONS + A2UI_CHART_INSTRUCTIONS;

    private static final String RAG_SYSTEM_PROMPT = """
            You are a helpful AI assistant with access to a knowledge base.
            Use the provided context to answer questions accurately.
            If the context doesn't contain enough information, say so.
            Always cite relevant sources from the context when available.

            """ + MARKDOWN_FORMATTING_INSTRUCTIONS + A2UI_CHART_INSTRUCTIONS;

    private static final String SUMMARIZATION_PROMPT = """
            Analyze the following text and provide a structured response.
            
            Text: {text}
            
            Respond with a JSON object containing:
            - summary: A brief summary of the text (max 50 words)
            - sentiment: One of POSITIVE, NEUTRAL, or NEGATIVE
            - key_points: 3-5 key takeaways from the text
            - entities: List of named entities (people, places, organizations) mentioned
            - language: The detected language of the text
            """;

    private static final String TRANSLATION_PROMPT = """
            Translate the following text to {targetLanguage}.
            
            Text: {text}
            
            Provide only the translation without any additional commentary.
            """;

    private static final String QUESTION_ANSWER_PROMPT = """
            Based on the following context, answer the question.
            
            Context:
            {context}
            
            Question: {question}
            
            If the context doesn't provide enough information to answer, say:
            "I cannot answer this question based on the provided context."
            """;

    /**
     * Gets the default system prompt.
     */
    public String getDefaultSystemPrompt() {
        return DEFAULT_SYSTEM_PROMPT;
    }

    /**
     * Gets the RAG system prompt for knowledge base queries.
     */
    public String getRagSystemPrompt() {
        return RAG_SYSTEM_PROMPT;
    }

    /**
     * Builds a summarization prompt for text analysis.
     *
     * @param text The text to summarize
     * @return Formatted summarization prompt
     */
    public String buildSummarizationPrompt(String text) {
        log.debug("Building summarization prompt for text of length: {}", text.length());
        return SUMMARIZATION_PROMPT.replace("{text}", text);
    }

    /**
     * Builds a translation prompt.
     *
     * @param text The text to translate
     * @param targetLanguage The target language
     * @return Formatted translation prompt
     */
    public String buildTranslationPrompt(String text, String targetLanguage) {
        log.debug("Building translation prompt to {}", targetLanguage);
        return TRANSLATION_PROMPT
                .replace("{text}", text)
                .replace("{targetLanguage}", targetLanguage);
    }

    /**
     * Builds a question-answering prompt for RAG.
     *
     * @param context The retrieved context
     * @param question The user's question
     * @return Formatted Q&A prompt
     */
    public String buildQuestionAnswerPrompt(String context, String question) {
        log.debug("Building Q&A prompt with context length: {}", context.length());
        return QUESTION_ANSWER_PROMPT
                .replace("{context}", context)
                .replace("{question}", question);
    }

    /**
     * Builds a system prompt with custom instructions.
     *
     * @param customInstructions Additional instructions to add
     * @return Combined system prompt
     */
    public String buildCustomSystemPrompt(String customInstructions) {
        return DEFAULT_SYSTEM_PROMPT + "\n\n" + customInstructions;
    }
}
