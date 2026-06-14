package com.ai.application.tool;

import java.util.Map;

/**
 * Result of a tool execution.
 */
public final class ToolResult {

    private final String content;
    private final boolean isError;
    private final Map<String, Object> structured;

    public ToolResult(String content, boolean isError) {
        this(content, isError, null);
    }

    public ToolResult(String content, boolean isError, Map<String, Object> structured) {
        this.content = content;
        this.isError = isError;
        this.structured = structured;
    }

    public static ToolResult success(String content) {
        return new ToolResult(content, false);
    }

    public static ToolResult success(String content, Map<String, Object> structured) {
        return new ToolResult(content, false, structured);
    }

    public static ToolResult error(String content) {
        return new ToolResult(content, true);
    }

    public String content() { return content; }
    public boolean isError() { return isError; }
    public Map<String, Object> structured() { return structured; }

    @Override
    public String toString() {
        return "ToolResult{isError=" + isError + ", contentLength=" + (content != null ? content.length() : 0) + "}";
    }
}
