package com.ai.tools.application.usecase;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.tools.domain.model.ToolResult;
import com.ai.tools.domain.model.WeatherReport;
import com.ai.tools.domain.vo.WeatherQuery;
import com.ai.tools.infrastructure.tools.WeatherTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolsFacade")
class ToolsFacadeTest {

    @Mock
    private ChatClientProvider chatClientProvider;

    @Mock
    private WeatherTools weatherTools;

    @Mock
    private WeatherReport weatherReport;

    @Mock
    private DocumentSearchTool documentSearchTool;

    @Mock
    private WebSearchTool webSearchTool;

    private ToolsFacade toolsFacade;

    @BeforeEach
    void setUp() {
        toolsFacade = new ToolsFacade(
                chatClientProvider, weatherTools, weatherReport, documentSearchTool, webSearchTool);
    }

    @Test
    @DisplayName("should return weather for known city")
    void should_return_weather_for_known_city() {
        when(weatherReport.lookupCurrent(any(WeatherQuery.class)))
                .thenReturn(ToolResult.success("北京今天的天气：温度 25°C，天气 晴，湿度 65%"));

        String weather = toolsFacade.getWeather("beijing");

        assertThat(weather).contains("北京");
    }

    @Test
    @DisplayName("should delegate document search to port")
    void should_delegate_document_search_to_port() {
        when(documentSearchTool.searchDocuments("query", null)).thenReturn("docs");

        assertThat(toolsFacade.searchDocuments("query", null)).isEqualTo("docs");
    }
}
