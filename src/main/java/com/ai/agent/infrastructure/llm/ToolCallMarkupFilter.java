package com.ai.agent.infrastructure.llm;

import java.util.regex.Pattern;

/**
 * Strips DeepSeek DSML tool-call markup from model text.
 * Matches any angle-bracket tag containing {@code DSML}, including fullwidth
 * {@code ｜} and oddly spaced variants.
 *
 * @see <a href="https://huggingface.co/deepseek-ai/DeepSeek-V4-Pro/blob/main/encoding/README.md">DeepSeek DSML</a>
 */
final class ToolCallMarkupFilter {

    private static final Pattern DSML_PAIR_BLOCK = Pattern.compile(
            "<\\s*[^<>]*DSML[^<>]*>[\\s\\S]*?</\\s*[^<>]*DSML[^<>]*>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DSML_UNCLOSED = Pattern.compile(
            "<\\s*[^<>]*DSML[^<>]*>[\\s\\S]*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DSML_ANY_TAG = Pattern.compile(
            "</?\\s*[^<>]*DSML[^<>]*>",
            Pattern.CASE_INSENSITIVE);

    private ToolCallMarkupFilter() {
    }

    static String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String cleaned = DSML_PAIR_BLOCK.matcher(content).replaceAll("");
        cleaned = DSML_UNCLOSED.matcher(cleaned).replaceAll("");
        cleaned = DSML_ANY_TAG.matcher(cleaned).replaceAll("");
        return cleaned.replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    static boolean looksLikeToolMarkup(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        String lower = content.toLowerCase();
        return lower.contains("dsml") || lower.contains("tool_calls");
    }
}
