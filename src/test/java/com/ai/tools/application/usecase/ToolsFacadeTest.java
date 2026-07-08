package com.ai.tools.application.usecase;

import com.ai.common.domain.port.out.DocumentSearchTool;
import com.ai.common.domain.port.out.WebSearchTool;
import com.ai.tools.infrastructure.tools.WeatherTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolsFacade")
class ToolsFacadeTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private WeatherTools weatherTools;

    @Mock
    private DocumentSearchTool documentSearchTool;

    @Mock
    private WebSearchTool webSearchTool;

    private ToolsFacade toolsFacade;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        toolsFacade = new ToolsFacade(chatClientBuilder, weatherTools, documentSearchTool, webSearchTool);
    }

    @Test
    @DisplayName("should return weather for known city")
    void should_return_weather_for_known_city() {
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
