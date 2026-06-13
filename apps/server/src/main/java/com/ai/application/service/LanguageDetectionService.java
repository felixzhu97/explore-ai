package com.ai.application.service;

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

                ## 格式要求（必须严格遵守）

                - 标题符号后必须加空格：`# 标题` 而不是 `#标题`
                - 粗体符号前后必须加空格：`** 关键词 **` 而不是 `**关键词**`
                - 列表符号后必须加空格：`- 要点` 而不是 `-要点`
                - **段落之间必须用两个换行符分隔（空一行）**：每个标题、每个列表项之间都要有空行
                - **每个段落（标题或正文）的尾部必须加 `\n\n`**
                - 使用标准 Markdown 语法

                ## 正确示例（严格模仿此格式，每个段落尾部都有空行）

                # 概述\n\n

                **人工智能**（AI）是计算机科学的重要分支。\n\n

                # 主要特点\n\n

                - **机器学习**：让计算机从数据中学习\n
                - **自然语言处理**：理解和生成人类语言\n\n

                # 应用领域\n\n

                AI 广泛应用于医疗、金融、教育等行业。\n\n

                # 回答

                """;
            case "ja" -> """
                あなたは有帮助なアシスタントです。以下のドキュメントの内容に基づいて、構造化されたマークダウン形式でユーザーの質問に回答してください。

                # ドキュメント内容
                %s

                # ユーザーの質問
                %s

                ## フォーマット要件（厳守必須）

                - 見出し記号の後にスペースが必要：`# 見出し` 而不是 `#見出し`
                - 太字記号の前後にスペースが必要：`** キーワード **` 而不是 `**キーワード**`
                - リスト記号の後にスペースが必要：`- 要点` 而不是 `-要点`
                - **段落の間に必ず空行を挿入（2つの改行で区切る）**：各見出し、リスト項目同士は必ず空行で分隔
                - **各段落（見出しまたは本文）の末尾に `\n\n` を追加すること**
                - 標準的な Markdown 構文を使用

                ## 正しい例（この形式を厳密に真似ること、各段落の末尾に空行がある）

                # 概要\n\n

                **人工知能**（AI）はコンピュータ科学の重要な分支です。\n\n

                # 主な特徴\n\n

                - **機械学習**：コンピュータにデータから学習させる\n
                - **自然言語処理**：人間の言語を理解し生成する\n\n



                # 応用分野\n\n

                AI は医療、金融、教育などの分野で広く応用されています。\n\n

                # 回答

                """;
            default -> """
                You are a helpful assistant. Answer the user's question using structured Markdown.

                # Context
                %s

                # Question
                %s

                ## Format Requirements (MUST STRICTLY FOLLOW)

                - Space after heading symbols: `# Heading` NOT `#Heading`
                - Space around bold symbols: `** keyword **` NOT `**keyword**`
                - Space after list symbols: `- Bullet point` NOT `-Bullet point`
                - **Blank line between paragraphs (use two line breaks to separate)**: Always add empty line between headings and lists
                - **Add `\n\n` at the end of every paragraph (heading or body text)**
                - Use standard Markdown syntax

                ## Correct Example (STRICTLY follow this format, each paragraph ends with blank line)

                # Overview\n\n

                **Artificial Intelligence** (AI) is an important branch of computer science.\n\n



                # Key Features\n\n

                - **Machine Learning**: Enabling computers to learn from data\n
                - **Natural Language Processing**: Understanding and generating human language\n\n



                # Application Areas\n\n

                AI is widely applied in healthcare, finance, education, and other industries.\n\n

                # Answer

                """;
        };
    }
}
