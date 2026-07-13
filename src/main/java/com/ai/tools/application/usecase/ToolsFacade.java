package com.ai.tools.application.usecase;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.observability.AiMetricsRecorder;
import com.ai.common.util.LogSanitizer;
import com.ai.tools.domain.model.WeatherReport;
import com.ai.tools.domain.vo.WeatherForecast;
import com.ai.tools.domain.vo.WeatherQuery;
import com.ai.tools.infrastructure.tools.WeatherTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ToolsFacade {

    private static final Logger log = LoggerFactory.getLogger(ToolsFacade.class);

    private final ChatClientProvider chatClientProvider;
    private final WeatherTools weatherTools;
    private final WeatherReport weatherReport;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;
    private final AiMetricsRecorder metricsRecorder;

    public ToolsFacade(
            ChatClientProvider chatClientProvider,
            WeatherTools weatherTools,
            WeatherReport weatherReport,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool,
            AiMetricsRecorder metricsRecorder) {
        this.chatClientProvider = chatClientProvider;
        this.weatherTools = weatherTools;
        this.weatherReport = weatherReport;
        this.documentSearchTool = documentSearchTool;
        this.webSearchTool = webSearchTool;
        this.metricsRecorder = metricsRecorder;
    }

    public String chatWithTools(String question) {
        return metricsRecorder.recordToolCall(() -> {
            log.info("ToolsFacade.chatWithTools: {}", LogSanitizer.truncate(question));
            ChatClient chatClient = chatClientProvider.createStateless(TextChatOptions.of("openai", null, true));
            return chatClient.prompt()
                    .user(question)
                    .call()
                    .content();
        });
    }

    public String getWeather(String city) {
        log.info("ToolsFacade.getWeather: {}", city);
        return weatherReport.lookupCurrent(WeatherQuery.of(city)).content();
    }

    public String getForecast(String city, Integer days) {
        log.info("ToolsFacade.getForecast: {} days={}", city, days);
        return weatherReport.generateForecast(WeatherForecast.of(WeatherQuery.of(city), days)).content();
    }

    public String searchDocuments(String query, List<String> docIds) {
        log.info("ToolsFacade.searchDocuments: {}", LogSanitizer.truncate(query));
        return documentSearchTool.searchDocuments(query, docIds);
    }

    public String listDocuments() {
        log.info("ToolsFacade.listDocuments");
        return documentSearchTool.listDocuments();
    }

    public String searchWeb(String query) {
        log.info("ToolsFacade.searchWeb: {}", LogSanitizer.truncate(query));
        return webSearchTool.searchWeb(query);
    }

}
