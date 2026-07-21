package com.ai.common.application.llm;

/**
 * Composition profiles for ChatClient assembly (Spring AI Advisors + tools).
 */
public enum ChatClientProfile {
    /** Memory + default system + tools/MCP when enabled. */
    MEMORY_TOOLS,
    /** Default system + tools/MCP, no chat memory. */
    TOOLS,
    /** Memory only, no tools (e.g. RAG answer after retrieval advisor). */
    MEMORY,
    /** Minimal client: no memory, no default system/tools. */
    BARE
}
