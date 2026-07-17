package com.ai.agent.infrastructure.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallMarkupFilterTest {

    @Test
    void should_strip_deepseek_fullwidth_dsml_tool_calls() {
        String raw = """
                开始调研。
                <｜DSML｜tool_calls>
                <｜DSML｜invoke name="searchWeb">
                <｜DSML｜parameter name="query" string="true">NVIDIA NIM agent 2025
                </｜DSML｜parameter>
                </｜DSML｜invoke>
                </｜DSML｜tool_calls>
                结论如下。
                """;

        String cleaned = ToolCallMarkupFilter.sanitize(raw);

        assertFalse(cleaned.contains("DSML"));
        assertFalse(cleaned.contains("searchWeb"));
        assertFalse(cleaned.contains("NVIDIA"));
        assertTrue(cleaned.contains("开始调研。"));
        assertTrue(cleaned.contains("结论如下。"));
    }

    @Test
    void should_strip_spaced_dsml_tags() {
        String raw = "hi < | DSML | tool_calls>q</ | DSML | tool_calls> bye";
        String cleaned = ToolCallMarkupFilter.sanitize(raw);
        assertFalse(cleaned.toLowerCase().contains("dsml"));
        assertTrue(cleaned.contains("hi"));
        assertTrue(cleaned.contains("bye"));
    }

    @Test
    void should_strip_ascii_pipe_variant() {
        String raw = """
                我将进行第一轮调研。
                <|DSML|tool_calls>
                <|DSML|invoke name="searchWeb">
                <|DSML|parameter name="query" string="true">2025 AI RAG trends
                </|DSML|parameter>
                </|DSML|invoke>
                </|DSML|tool_calls>
                以下是关键信号摘要。
                """;

        String cleaned = ToolCallMarkupFilter.sanitize(raw);

        assertFalse(cleaned.contains("DSML"));
        assertFalse(cleaned.contains("2025 AI RAG trends"));
        assertTrue(cleaned.contains("我将进行第一轮调研。"));
        assertTrue(cleaned.contains("以下是关键信号摘要。"));
    }

    @Test
    void should_return_empty_when_only_markup() {
        String raw = "<｜DSML｜tool_calls><｜DSML｜invoke name=\"searchWeb\"></｜DSML｜tool_calls>";
        assertEquals("", ToolCallMarkupFilter.sanitize(raw));
    }

    @Test
    void should_detect_tool_markup() {
        assertTrue(ToolCallMarkupFilter.looksLikeToolMarkup("<｜DSML｜tool_calls>"));
        assertFalse(ToolCallMarkupFilter.looksLikeToolMarkup("plain research summary"));
    }
}
