package com.ai.chat.infrastructure.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GfmSyntaxNormalizer")
class GfmSyntaxNormalizerTest {

    private final GfmSyntaxNormalizer normalizer = new GfmSyntaxNormalizer();

    @Test
    @DisplayName("should insert space after unordered list marker when missing")
    void shouldInsertSpaceAfterUnorderedListMarkerWhenMissing() {
        assertThat(normalizer.normalize("-**Label:** text")).isEqualTo("- **Label:** text");
    }

    @Test
    @DisplayName("should insert space after ATX heading marker when missing")
    void shouldInsertSpaceAfterAtxHeadingMarkerWhenMissing() {
        assertThat(normalizer.normalize("##Heading")).isEqualTo("## Heading");
    }

    @Test
    @DisplayName("should preserve horizontal rules")
    void shouldPreserveHorizontalRules() {
        assertThat(normalizer.normalize("---\n\nparagraph")).isEqualTo("---\n\nparagraph");
    }

    @Test
    @DisplayName("should not break numeric ranges")
    void shouldNotBreakNumericRanges() {
        assertThat(normalizer.normalize("period (1819-1942)")).isEqualTo("period (1819-1942)");
    }

    @Test
    @DisplayName("should promote outline section lines to ATX headings")
    void shouldPromoteOutlineSectionLinesToAtxHeadings() {
        assertThat(normalizer.normalize("一、古代至马六甲王朝（1511年）\n- item"))
                .contains("## 一、古代至马六甲王朝（1511年）");
    }

    @Test
    @DisplayName("should insert newline between section title and glued list item")
    void shouldInsertNewlineBetweenSectionTitleAndGluedListItem() {
        assertThat(normalizer.normalize("一、古代（1511年）-**早期**"))
                .contains("\n- **早期**");
    }

    @Test
    @DisplayName("should promote outline headings containing hyphens")
    void shouldPromoteOutlineHeadingsContainingHyphens() {
        assertThat(normalizer.normalize("一、Section - Heading"))
                .contains("## 一、Section - Heading");
    }

    @Test
    @DisplayName("should not modify fenced code blocks")
    void shouldNotModifyFencedCodeBlocks() {
        String input = """
                ```c
                #define MAX 1
                int value = -1;
                -verbose
                ```
                """;
        assertThat(normalizer.normalize(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("should preserve italic markers at line start")
    void shouldPreserveItalicMarkersAtLineStart() {
        assertThat(normalizer.normalize("*italic* text")).isEqualTo("*italic* text");
    }

    @Test
    @DisplayName("should preserve negative numbers at line start")
    void shouldPreserveNegativeNumbersAtLineStart() {
        assertThat(normalizer.normalize("-1 is negative")).isEqualTo("-1 is negative");
    }
}
