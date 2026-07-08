package com.ai.tools.application.usecase;

import com.ai.common.domain.port.out.DocumentSearchTool;
import com.ai.common.domain.port.out.WebSearchTool;
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

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;
    private final WeatherReport weatherReport;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;

    public ToolsFacade(
            ChatClient.Builder chatClientBuilder,
            WeatherTools weatherTools,
            DocumentSearchTool documentSearchTool,
            WebSearchTool webSearchTool) {
        this.chatClient = chatClientBuilder.build();
        this.weatherTools = weatherTools;
        this.weatherReport = new WeatherReport();
        this.documentSearchTool = documentSearchTool;
        this.webSearchTool = webSearchTool;
    }

    public String chatWithTools(String question) {
        log.info("ToolsFacade.chatWithTools: {}", truncate(question));
        return chatClient.prompt()
                .user(question)
                .tools(weatherTools, documentSearchTool, webSearchTool)
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
