package com.ai.mcp.infrastructure.server;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.rag.infrastructure.config.RagProperties;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.tools.infrastructure.tools.WeatherTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiMcpServerService Tests")
class AiMcpServerServiceTest {

    @Mock
    private WeatherTools weatherTools;

    @Mock
    private DocumentSearchTool documentSearchTool;

    @Mock
    private ChatUseCase aiChatUseCase;

    private final RagProperties ragProperties = new RagProperties();
    private AiMcpServerService service;

    @BeforeEach
    void setUp() {
        service = new AiMcpServerService(weatherTools, documentSearchTool, aiChatUseCase, ragProperties);
    }

    @Nested
    @DisplayName("getWeather")
    class GetWeatherTests {

        @Test
        @DisplayName("should return weather for valid city")
        void shouldReturnWeatherForValidCity() {
            String city = "Beijing";
            String expectedWeather = "Sunny, 25°C";
            when(weatherTools.getWeather(city)).thenReturn(expectedWeather);

            String result = service.getWeather(city);

            assertThat(result).isEqualTo(expectedWeather);
            verify(weatherTools).getWeather(city);
        }
    }

    @Nested
    @DisplayName("getForecast")
    class GetForecastTests {

        @Test
        @DisplayName("should return forecast for city with days")
        void shouldReturnForecastWithDays() {
            String city = "Shanghai";
            Integer days = 5;
            String expectedForecast = "5-day forecast: sunny, rainy, cloudy, sunny, cloudy";
            when(weatherTools.getForecast(city, days)).thenReturn(expectedForecast);

            String result = service.getForecast(city, days);

            assertThat(result).isEqualTo(expectedForecast);
            verify(weatherTools).getForecast(city, days);
        }

        @Test
        @DisplayName("should return forecast with null days")
        void shouldReturnForecastWithNullDays() {
            String city = "Guangzhou";
            String expectedForecast = "3-day forecast: sunny, cloudy, rainy";
            when(weatherTools.getForecast(city, null)).thenReturn(expectedForecast);

            String result = service.getForecast(city, null);

            assertThat(result).isEqualTo(expectedForecast);
            verify(weatherTools).getForecast(city, null);
        }
    }

    @Nested
    @DisplayName("searchKnowledgeBase")
    class SearchKnowledgeBaseTests {

        @Test
        @DisplayName("should search with query only")
        void shouldSearchWithQueryOnly() {
            String query = "artificial intelligence";
            String expectedResults = "[{\"text\": \"AI is...\", \"score\": 0.95}]";
            when(documentSearchTool.searchDocuments(query, null)).thenReturn(expectedResults);

            String result = service.searchKnowledgeBase(query, null);

            assertThat(result).isEqualTo(expectedResults);
            verify(documentSearchTool).searchDocuments(query, null);
        }

        @Test
        @DisplayName("should search with query and docIds")
        void shouldSearchWithQueryAndDocIds() {
            String query = "machine learning";
            String docIds = "doc1,doc2,doc3";
            String expectedResults = "[{\"text\": \"ML results...\", \"score\": 0.88}]";
            when(documentSearchTool.searchDocuments(query, List.of("doc1", "doc2", "doc3")))
                    .thenReturn(expectedResults);

            String result = service.searchKnowledgeBase(query, docIds);

            assertThat(result).isEqualTo(expectedResults);
            verify(documentSearchTool).searchDocuments(query, List.of("doc1", "doc2", "doc3"));
        }

        @Test
        @DisplayName("should handle empty docIds string")
        void shouldHandleEmptyDocIdsString() {
            String query = "deep learning";
            when(documentSearchTool.searchDocuments(query, null)).thenReturn("[]");

            String result = service.searchKnowledgeBase(query, "");

            assertThat(result).isEqualTo("[]");
            verify(documentSearchTool).searchDocuments(query, null);
        }
    }

    @Nested
    @DisplayName("listDocuments")
    class ListDocumentsTests {

        @Test
        @DisplayName("should list all documents")
        void shouldListAllDocuments() {
            String expectedDocs = "[{\"id\": \"doc1\", \"title\": \"Document 1\"}, {\"id\": \"doc2\", \"title\": \"Document 2\"}]";
            when(documentSearchTool.listDocuments()).thenReturn(expectedDocs);

            String result = service.listDocuments();

            assertThat(result).isEqualTo(expectedDocs);
            verify(documentSearchTool).listDocuments();
        }
    }

    @Nested
    @DisplayName("aiChat")
    class AiChatTests {

        @Test
        @DisplayName("should return AI response")
        void shouldReturnAiResponse() {
            String message = "Hello AI";
            String expectedResponse = "Hello! How can I help you?";
            when(aiChatUseCase.chat(message)).thenReturn(expectedResponse);

            String result = service.aiChat(message);

            assertThat(result).isEqualTo(expectedResponse);
            verify(aiChatUseCase).chat(message);
        }

        @Test
        @DisplayName("should truncate long message for logging")
        void shouldTruncateLongMessageForLogging() {
            String longMessage = "A".repeat(100);
            String expectedResponse = "Response";
            when(aiChatUseCase.chat(longMessage)).thenReturn(expectedResponse);

            String result = service.aiChat(longMessage);

            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    @Nested
    @DisplayName("getConfig")
    class GetConfigTests {

        @Test
        @DisplayName("should return chunk size config")
        void shouldReturnChunkSizeConfig() {
            String result = service.getConfig("spring.ai.rag.chunk.size");
            assertThat(result).isEqualTo("500");
        }

        @Test
        @DisplayName("should return chunk overlap config")
        void shouldReturnChunkOverlapConfig() {
            String result = service.getConfig("spring.ai.rag.chunk.overlap");
            assertThat(result).isEqualTo("50");
        }

        @Test
        @DisplayName("should return top-k config")
        void shouldReturnTopKConfig() {
            String result = service.getConfig("spring.ai.rag.retrieval.top-k");
            assertThat(result).isEqualTo("5");
        }

        @Test
        @DisplayName("should return score threshold config")
        void shouldReturnScoreThresholdConfig() {
            String result = service.getConfig("spring.ai.rag.retrieval.score-threshold");
            assertThat(result).isEqualTo("0.5");
        }

        @Test
        @DisplayName("should return error for unknown key")
        void shouldReturnErrorForUnknownKey() {
            String result = service.getConfig("unknown.key");
            assertThat(result).isEqualTo("Configuration key not found: unknown.key");
        }
    }
}
