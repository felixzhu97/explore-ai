package com.ai.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Lightweight language detection service using JDK standard library.
 * Detects primary language based on character distribution in input text.
 */
public class LanguageDetectionService {

    private static final Pattern CJK_UNIFIED_IDEOGRAPHS = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Pattern HIRAGANA = Pattern.compile("[\\u3040-\\u309f]");
    private static final Pattern KATAKANA = Pattern.compile("[\\u30a0-\\u30ff]");
    private static final Pattern KANJI = Pattern.compile("[\\u3400-\\u4dbf\\u4e00-\\u9fff]");
    private static final Pattern LATIN = Pattern.compile("[a-zA-Z]");

    private static final double JAPANESE_KANA_THRESHOLD = 0.05;
    private static final double CHINESE_CJK_THRESHOLD = 0.3;
    private static final double ENGLISH_LATIN_THRESHOLD = 0.5;

    /**
     * Detects the primary language of the given text.
     *
     * @param text the input text to analyze
     * @return language code: "zh" for Chinese, "ja" for Japanese, "en" for English, "default" otherwise
     */
    public String detect(String text) {
        if (text == null || text.isBlank()) {
            return "default";
        }

        int cjkCount = countMatches(text, CJK_UNIFIED_IDEOGRAPHS);
        int hiraganaCount = countMatches(text, HIRAGANA);
        int katakanaCount = countMatches(text, KATAKANA);
        int kanjiCount = countMatches(text, KANJI);
        int latinCount = countMatches(text, LATIN);

        int totalChars = text.length();
        if (totalChars == 0) {
            return "default";
        }

        int japaneseKanaCount = hiraganaCount + katakanaCount;
        boolean hasJapaneseContent = japaneseKanaCount > 0 || kanjiCount > 0;
        boolean isPredominantlyJapanese = hasJapaneseContent && japaneseKanaCount > totalChars * JAPANESE_KANA_THRESHOLD;
        boolean isPredominantlyChinese = cjkCount > totalChars * CHINESE_CJK_THRESHOLD && !isPredominantlyJapanese;
        boolean isPredominantlyEnglish = latinCount > totalChars * ENGLISH_LATIN_THRESHOLD;

        if (isPredominantlyJapanese) {
            return "ja";
        } else if (isPredominantlyChinese) {
            return "zh";
        } else if (isPredominantlyEnglish) {
            return "en";
        }

        return "default";
    }

    private int countMatches(String text, Pattern pattern) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Builds a prompt based on detected language.
     *
     * @param question the user's question
     * @param context the retrieved context from documents
     * @param languageCode the detected language code
     * @return the formatted prompt in the detected language
     */
    public String buildPrompt(String question, String context, String languageCode) {
        if (context == null || context.isBlank()) {
            return getNoContextMessage(languageCode);
        }

        String template = getPromptTemplate(languageCode);
        return String.format(template, context, question);
    }

    private String getNoContextMessage(String languageCode) {
        return switch (languageCode) {
            case "zh" -> "没有找到相关的文档来回答您的问题。请先上传一些文档。";
            case "ja" -> "関連ドキュメントがありません。まずドキュメントをアップロードしてください。";
            default -> "I don't have relevant documents to answer your question. Please upload some documents first.";
        };
    }

    private String getPromptTemplate(String languageCode) {
        return loadPromptTemplate(languageCode);
    }

    private String loadPromptTemplate(String languageCode) {
        String resourcePath = switch (languageCode) {
            case "zh" -> "prompts/rag-chat-zh.txt";
            case "ja" -> "prompts/rag-chat-ja.txt";
            default -> "prompts/rag-chat-en.txt";
        };
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Prompt template not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt template: " + resourcePath, e);
        }
    }
}
