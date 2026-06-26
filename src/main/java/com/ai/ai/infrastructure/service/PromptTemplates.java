package com.ai.ai.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infrastructure service for centralized prompt templates.
 * Provides consistent prompt patterns across different use cases.
 */
public class PromptTemplates {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplates.class);

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful AI assistant. Provide accurate and concise responses.
            """;

    private static final String RAG_SYSTEM_PROMPT = """
            You are a helpful AI assistant with access to a knowledge base.
            Use the provided context to answer questions accurately.
            If the context doesn't contain enough information, say so.
            Always cite relevant sources from the context when available.
            """;

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
