package com.ai.tools.application.usecase;

import com.ai.chat.application.usecase.TextChatOptions;
import com.ai.chat.application.usecase.ChatClientProvider;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WebSearchTool;
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

    public ToolsFacade(
            ChatClientProvider chatClientProvider,
            WeatherTools weatherTools,
            WeatherReport weatherReport,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool) {
        this.chatClientProvider = chatClientProvider;
        this.weatherTools = weatherTools;
        this.weatherReport = weatherReport;
        this.documentSearchTool = documentSearchTool;
        this.webSearchTool = webSearchTool;
    }

    public String chatWithTools(String question) {
        log.info("ToolsFacade.chatWithTools: {}", truncate(question));
        ChatClient chatClient = chatClientProvider.createStateless(TextChatOptions.of("openai", null, true));
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
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
        log.info("ToolsFacade.searchDocuments: {}", truncate(query));
        return documentSearchTool.searchDocuments(query, docIds);
    }

    public String listDocuments() {
        log.info("ToolsFacade.listDocuments");
        return documentSearchTool.listDocuments();
    }

    public String searchWeb(String query) {
        log.info("ToolsFacade.searchWeb: {}", truncate(query));
        return webSearchTool.searchWeb(query);
    }

    private String truncate(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= 50) {
            return text;
        }
        return text.substring(0, 50) + "...";
    }
}
