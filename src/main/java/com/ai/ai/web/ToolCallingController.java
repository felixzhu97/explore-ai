package com.ai.ai.web;

import com.ai.common.streaming.StreamingService;
import com.ai.rag.infrastructure.tools.RagSearchTool;
import com.ai.ai.infrastructure.tools.WeatherTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Tool Calling controller demonstrating Spring AI 2.0 function calling.
 */
@RestController
@RequestMapping("/api/tools")
@Tag(name = "Tool Calling", description = "AI function calling with tools")
public class ToolCallingController {

    private static final Logger log = LoggerFactory.getLogger(ToolCallingController.class);

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;
    private final RagSearchTool ragSearchTool;
    private final StreamingService streamingService;

    public ToolCallingController(
            ChatClient.Builder chatClientBuilder,
            WeatherTools weatherTools,
            RagSearchTool ragSearchTool,
            StreamingService streamingService) {
        this.chatClient = chatClientBuilder.build();
        this.weatherTools = weatherTools;
        this.ragSearchTool = ragSearchTool;
        this.streamingService = streamingService;
    }

    @GetMapping("/weather")
    @Operation(summary = "Get weather for a city")
    public String getWeather(@RequestParam String city) {
        return weatherTools.getWeather(city);
    }

    @GetMapping("/weather/forecast")
    @Operation(summary = "Get weather forecast for a city")
    public String getForecast(
            @RequestParam String city,
            @RequestParam(required = false) Integer days
    ) {
        return weatherTools.getForecast(city, days);
    }

    @GetMapping("/documents/search")
    @Operation(summary = "Search documents in knowledge base")
    public String searchDocuments(
            @RequestParam String query,
            @RequestParam(required = false) String docIds
    ) {
        List<String> docIdList = null;
        if (docIds != null && !docIds.isBlank()) {
            docIdList = List.of(docIds.split(","));
        }

        return ragSearchTool.searchDocuments(query, docIdList);
    }

    @GetMapping("/documents/list")
    @Operation(summary = "List all documents in knowledge base")
    public String listDocuments() {
        return ragSearchTool.listDocuments();
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Chat with function calling (streaming)")
    public Flux<ServerSentEvent<String>> chatWithToolsStream(@RequestBody ToolChatRequest request) {
        try {
            String response = chatClient.prompt()
                    .user(request.question())
                    .tools(weatherTools, ragSearchTool)
                    .call()
                    .content();

            return streamingService.streamWords(response);

        } catch (Exception e) {
            log.error("Error in streaming chat with tools", e);
            return Flux.error(e);
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with function calling")
    public ToolChatResponse chatWithTools(@RequestBody ToolChatRequest request) {
        try {
            String response = chatClient.prompt()
                    .user(request.question())
                    .tools(weatherTools, ragSearchTool)
                    .call()
                    .content();

            return new ToolChatResponse(response, null);

        } catch (Exception e) {
            log.error("Error in chat with tools", e);
            return new ToolChatResponse("抱歉，处理您的请求时发生错误，请稍后重试。", null);
        }
    }

    public record ToolChatRequest(String question, List<String> docIds) {
    }

    public record ToolChatResponse(String answer, List<String> toolCalls) {
    }
}
