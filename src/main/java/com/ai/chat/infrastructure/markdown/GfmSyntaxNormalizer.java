package com.ai.chat.infrastructure.markdown;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repairs common GFM syntax spacing issues (e.g. "-item" → "- item") without changing content semantics.
 * See GitHub Flavored Markdown spec for list and heading token rules.
 */
@Component
public class GfmSyntaxNormalizer {

    private static final String HR_PLACEHOLDER = "\uE000HR\uE001";
    private static final String CODE_PLACEHOLDER_PREFIX = "\uE002CODE";
    private static final String CODE_PLACEHOLDER_SUFFIX = "\uE003";
    private static final Pattern HORIZONTAL_RULE = Pattern.compile("(?m)^([-*_]){3,}\\s*$");
    private static final Pattern CODE_BLOCK = Pattern.compile("(?s)```.*?```|~~~.*?~~~");

    public String normalize(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        List<String> codeBlocks = new ArrayList<>();
        Matcher codeMatcher = CODE_BLOCK.matcher(content);
        StringBuilder codeProtected = new StringBuilder();
        while (codeMatcher.find()) {
            codeBlocks.add(codeMatcher.group());
            codeMatcher.appendReplacement(
                    codeProtected,
                    CODE_PLACEHOLDER_PREFIX + (codeBlocks.size() - 1) + CODE_PLACEHOLDER_SUFFIX);
        }
        codeMatcher.appendTail(codeProtected);

        List<String> horizontalRules = new ArrayList<>();
        Matcher hrMatcher = HORIZONTAL_RULE.matcher(codeProtected.toString());
        StringBuilder protectedContent = new StringBuilder();
        while (hrMatcher.find()) {
            horizontalRules.add(hrMatcher.group());
            hrMatcher.appendReplacement(
                    protectedContent,
                    HR_PLACEHOLDER + (horizontalRules.size() - 1));
        }
        hrMatcher.appendTail(protectedContent);

        String normalized = protectedContent.toString();
        normalized = normalized.replaceAll("(?m)^(#{1,6})([^\\s#\\n])", "$1 $2");
        normalized = normalized.replaceAll("(?m)^(\\d+\\.)([^\\s\\n])", "$1 $2");
        normalized = normalized.replaceAll("([：。！？；:])(#{1,6}\\s)", "$1\n$2");
        normalized = normalized.replaceAll("([：。！？；:])-(?!-)(?=[^\\s\\n0-9a-zA-Z])", "$1\n- ");
        normalized = normalized.replaceAll("([：。！？；:])(\\d+\\.)(?=[^\\s\\n])", "$1\n$2 ");
        normalized = normalized.replaceAll(
                "([一二三四五六七八九十]+、[^\\n-]+)(-(?!-)(?=[^\\s\\n0-9a-zA-Z]))",
                "$1\n$2");
        normalized = promoteOutlineSectionHeadings(normalized);
        normalized = normalized.replaceAll("(?m)^-(?!-)(?=[^\\s\\n0-9a-zA-Z])", "- ");
        normalized = normalized.replaceAll("(?m)^\\*(?!\\*)(?=[^\\s\\n0-9a-zA-Z])", "* ");
        normalized = normalized.replaceAll("(?m)^\\+(?=[^\\s\\n0-9a-zA-Z])", "+ ");

        for (int index = 0; index < horizontalRules.size(); index++) {
            normalized = normalized.replace(HR_PLACEHOLDER + index, horizontalRules.get(index));
        }

        for (int index = 0; index < codeBlocks.size(); index++) {
            normalized = normalized.replace(
                    CODE_PLACEHOLDER_PREFIX + index + CODE_PLACEHOLDER_SUFFIX,
                    codeBlocks.get(index));
        }

        return normalized;
    }

    private String promoteOutlineSectionHeadings(String content) {
        return content.replaceAll("(?m)(^|\\n)([一二三四五六七八九十]+、[^\\n]+)(?=\\n|$)", "$1## $2");
    }
}
