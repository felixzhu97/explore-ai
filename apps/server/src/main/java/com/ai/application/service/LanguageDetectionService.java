package com.ai.application.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Lightweight language detection service using JDK standard library.
 * Detects primary language based on character distribution in input text.
 */
@Component
public class LanguageDetectionService {

    private static final Pattern CJK_UNIFIED_IDEOGRAPHS = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Pattern HIRAGANA = Pattern.compile("[\\u3040-\\u309f]");
    private static final Pattern KATAKANA = Pattern.compile("[\\u30a0-\\u30ff]");
    private static final Pattern KANJI = Pattern.compile("[\\u3400-\\u4dbf\\u4e00-\\u9fff]");
    private static final Pattern LATIN = Pattern.compile("[a-zA-Z]");

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
        boolean isPredominantlyJapanese = hasJapaneseContent && japaneseKanaCount > totalChars * 0.05;
        boolean isPredominantlyChinese = cjkCount > totalChars * 0.3 && !isPredominantlyJapanese;
        boolean isPredominantlyEnglish = latinCount > totalChars * 0.5;

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
            case "ja" -> "您的質問にお答えできる関連ドキュメントがありません。まずドキュメントをアップロードしてください。";
            default -> "I don't have relevant documents to answer your question. Please upload some documents first.";
        };
    }

    private String getPromptTemplate(String languageCode) {
        return switch (languageCode) {
            case "zh" -> """
                你是一个有用的助手。请根据以下文档内容，用结构化的 Markdown 格式回答用户的问题。

                # 文档内容
                %s

                # 用户问题
                %s

                ## 回答指南
                - 请使用中文回答
                - 根据内容使用 **粗体**、*斜体*、列表和代码块
                - 将不同的观点分段落陈述（段落之间留空行）
                - 使用 ## 标题来组织您的回答结构
                - 不要将所有内容写在一个段落中
                - 如果答案不在文档内容中，请明确说明

                # 回答""";
            case "ja" -> """
                あなたは有帮助なアシスタントです。以下のドキュメントの内容に基づいて、構造化されたマークダウン形式でユーザーの質問に回答してください。

                # ドキュメント内容
                %s

                # ユーザーの質問
                %s

                ## 回答ガイドライン
                - 日本語で回答してください
                - 内容に応じて **太字**、*斜体*、リスト、コードブロックを使用してください
                - 異なるアイデアは段落に分けてください（段落間に空行を置いてください）
                - ## 見出しを使用して回答を構造化してください
                - すべてを1つの段落に書かないでください
                - 答えがドキュメント内容にない場合は、その旨を明確に述べてください

                # 回答""";
            default -> """
                You are a helpful assistant. Answer the user's question using structured Markdown.

                # Context
                %s

                # Question
                %s

                ## Answer Guidelines
                - Use **bold**, *italic*, lists, and code blocks as appropriate
                - Separate ideas into paragraphs (leave blank lines between them)
                - Use ## headings to structure your response
                - Do NOT write everything in one paragraph
                - If the answer is not in the context, say so clearly

                # Answer""";
        };
    }
}
