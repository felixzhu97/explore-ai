package com.ai.common.infrastructure.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infrastructure service for centralized prompt templates.
 * Provides consistent prompt patterns across different use cases.
 */
public class PromptTemplates {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplates.class);

    private static final String MARKDOWN_FORMATTING_INSTRUCTIONS = """
            Format responses using GitHub Flavored Markdown (GFM):
            - Use ATX headings (# through ###) with one space after the hash marks
            - Put each heading and list on its own line; separate blocks with a blank line
            - Use unordered lists with "- " (hyphen followed by a space)
            - Use **bold** for emphasis within list items and paragraphs
            - Do not wrap the entire response in a code block unless asked

            Example:
            # Document Title

            ## 一、Section Heading (1511—1957)

            - **Label:** description text
            """;

    /**
     * A2UI v0.9 chart surfaces: NDJSON in ```a2ui fences (not raw ECharts option JSON).
     * When Tools are enabled, prefer searchWeb for live numbers before filling chartData.
     * Includes bilingual (ZH/EN) intent triggers so explicit Chinese requests are recognized.
     */
    private static final String A2UI_CHART_INSTRUCTIONS = """

            Intent routing (Chinese or English — treat as hard requirements when present):
            - Online search: 在线搜索 / 联网搜索 / 搜索 / 查一下 / search / look up / google
              → you MUST call searchWeb exactly once before answering with numbers
            - Chart drawing: 柱状图 / 条形图 / 折线图 / 饼图 / 图表 / 画图 / 可视化 /
              bar chart / line chart / pie chart / plot / visualize
              → you MUST emit an ```a2ui fence with Chart after any brief markdown
            - Chart type map: 柱状图/条形图→bar; 折线图→line; 饼图→pie; 圆环图→doughnut
            - Cite sources: 注明来源 / 引用 / 来源 / cite / sources / references
              → after the chart, list source URLs from search results in markdown
            - Market share / 市场份额 / 份额 / EV / 电动汽车 / brands named by the user
              are typical live-data + chart requests — search then chart, do not refuse

            Workflow for search + chart (one turn):
            1) Call searchWeb once with an English-focused query that includes year,
               quarter/region, metric, and brand names (e.g. "2025 Q1 global EV market
               share Tesla BYD Volkswagen")
            2) Do NOT call any tool again
            3) Reply in the user's language for markdown prose
            4) Emit the a2ui Chart (chartData from tool results only)
            5) Cite sources under a "## 来源" / "## Sources" heading

            When a chart or visualization helps (trends, comparisons, distribution),
            emit an A2UI surface after a short markdown explanation using a fenced
            block labeled a2ui. Inside the fence, put one A2UI JSON message per line
            (NDJSON). Rules:
            - Set "version": "v0.9" on every message
            - First message: createSurface with catalogId
              "https://explore-ai.local/catalogs/chat-v0.9" and a surfaceId
            - Then updateComponents with a flat adjacency list that includes id "root"
              and a Chart component (type: bar|line|pie|doughnut; title; chartData as
              [{label,value}, ...] literal or data path)
            - Optional updateDataModel for bound chartData paths
            - Do NOT output executable JavaScript or bare ECharts option JSON
            - Do NOT invent other custom components beyond this catalog (incl. Chart)
            - Prefer short brand names as chartData labels (Tesla, BYD, VW)

            Live / online data + charts (when tools such as searchWeb are available):
            - If the user asks to search online OR needs up-to-date statistics,
              rankings, prices, population, market shares, election results, or other
              figures you do not know, call searchWeb exactly once first
            - After searchWeb returns, do NOT call searchWeb or any other tool again;
              immediately write a short markdown explanation, then the a2ui chart fence
            - From the tool results, extract concrete numeric series into chartData
              as [{label, value}, ...] (numbers only for value; short labels)
            - Do NOT invent or guess chart numbers when search results are available;
              if results lack usable numbers, say so in markdown and skip the chart
              (or ask a clarifying question) instead of fabricating data
            - After the chart, briefly cite source URLs from the search results in
              markdown (do not put URLs inside the a2ui fence)

            Example Chinese request → expected behavior:
            User: "进行在线搜索，2025 年第一季度全球电动汽车市场各品牌份额
            （特斯拉、比亚迪、大众），然后绘制柱状图并注明来源。"
            → searchWeb once with "2025 Q1 global EV market share Tesla BYD Volkswagen"
            → markdown summary in Chinese + ```a2ui Chart type bar + "## 来源" links

            Example fence (abbreviated; real output must be valid NDJSON lines):
            ```a2ui
            {"version":"v0.9","createSurface":{"surfaceId":"s1","catalogId":"https://explore-ai.local/catalogs/chat-v0.9"}}
            {"version":"v0.9","updateComponents":{"surfaceId":"s1","components":[{"id":"root","component":"Column","children":["c1"]},{"id":"c1","component":"Chart","type":"bar","title":"2025 Q1 EV Share","chartData":[{"label":"BYD","value":10.5},{"label":"Tesla","value":7.5},{"label":"VW","value":2}]}]}}
            ```
            """;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful AI assistant. Provide accurate and concise responses.
            Follow the user's language for prose (Chinese in → Chinese out).
            When the user asks to search online and/or draw a chart, follow the
            search + a2ui workflow below — do not ignore those intents.

            When tools are available, use them to improve correctness:
            - Web search: current events, live facts, weather, prices, or anything needing up-to-date information
            - Document search: project knowledge base
            - Fetch (MCP): after you have a concrete URL, fetch the full page content before summarizing
            Prefer web search first; then Fetch when the user needs depth from a specific URL.
            Cite real source URLs from tool results. Do not invent tool results.
            If a tool fails or is unavailable, say so briefly.
            Do not call tools for casual chat that needs no external data.

            """ + MARKDOWN_FORMATTING_INSTRUCTIONS + A2UI_CHART_INSTRUCTIONS;

    private static final String RAG_SYSTEM_PROMPT = """
            You are a helpful AI assistant with access to a knowledge base.
            Use the provided context to answer questions accurately.
            If the context doesn't contain enough information, say so.
            Always cite relevant sources from the context when available.

            """ + MARKDOWN_FORMATTING_INSTRUCTIONS + A2UI_CHART_INSTRUCTIONS;

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
