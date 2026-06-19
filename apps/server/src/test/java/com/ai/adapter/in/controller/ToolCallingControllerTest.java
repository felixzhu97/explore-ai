package com.ai.adapter.in.controller;

import com.ai.adapter.out.streaming.StreamingService;
import com.ai.adapter.out.tools.RagSearchTool;
import com.ai.adapter.out.tools.WeatherTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolCallingController")
class ToolCallingControllerTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private WeatherTools weatherTools;

    @Mock
    private RagSearchTool ragSearchTool;

    @Mock
    private StreamingService streamingService;

    private ToolCallingController controller;

    private void createController() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        controller = new ToolCallingController(chatClientBuilder, weatherTools, ragSearchTool, streamingService);
    }

    @Nested
    @DisplayName("getWeather")
    class GetWeather {

        @Test
        @DisplayName("should return weather for city")
        void shouldReturnWeatherForCity() {
            createController();
            when(weatherTools.getWeather("beijing")).thenReturn("北京今天晴，25°C");

            String result = controller.getWeather("beijing");

            assertThat(result).isEqualTo("北京今天晴，25°C");
            verify(weatherTools).getWeather("beijing");
        }
    }

    @Nested
    @DisplayName("getForecast")
    class GetForecast {

        @Test
        @DisplayName("should return forecast for city")
        void shouldReturnForecastForCity() {
            createController();
            when(weatherTools.getForecast("beijing", 3)).thenReturn("北京未来3天：晴，多云，雨");

            String result = controller.getForecast("beijing", 3);

            assertThat(result).isEqualTo("北京未来3天：晴，多云，雨");
            verify(weatherTools).getForecast("beijing", 3);
        }

        @Test
        @DisplayName("should return forecast with default days")
        void shouldReturnForecastWithDefaultDays() {
            createController();
            when(weatherTools.getForecast("shanghai", null)).thenReturn("上海未来3天");

            String result = controller.getForecast("shanghai", null);

            verify(weatherTools).getForecast("shanghai", null);
        }
    }

    @Nested
    @DisplayName("searchDocuments")
    class SearchDocuments {

        @Test
        @DisplayName("should search documents")
        void shouldSearchDocuments() {
            createController();
            when(ragSearchTool.searchDocuments("test query", null))
                    .thenReturn("Found 2 results");

            String result = controller.searchDocuments("test query", null);

            assertThat(result).isEqualTo("Found 2 results");
            verify(ragSearchTool).searchDocuments("test query", null);
        }

        @Test
        @DisplayName("should search documents with docIds")
        void shouldSearchDocumentsWithDocIds() {
            createController();
            when(ragSearchTool.searchDocuments("test", List.of("doc1", "doc2")))
                    .thenReturn("Found results");

            String result = controller.searchDocuments("test", "doc1,doc2");

            assertThat(result).isEqualTo("Found results");
            verify(ragSearchTool).searchDocuments("test", List.of("doc1", "doc2"));
        }
    }

    @Nested
    @DisplayName("listDocuments")
    class ListDocuments {

        @Test
        @DisplayName("should list all documents")
        void shouldListAllDocuments() {
            createController();
            when(ragSearchTool.listDocuments()).thenReturn("Document 1\nDocument 2");

            String result = controller.listDocuments();

            assertThat(result).isEqualTo("Document 1\nDocument 2");
            verify(ragSearchTool).listDocuments();
        }
    }
}
