package com.ai.tools.web;

import com.ai.tools.application.usecase.ToolsFacade;
import com.ai.tools.domain.vo.ToolCatalogEntry;
import com.ai.tools.domain.vo.ToolSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolsController")
class ToolsControllerTest {

    @Mock
    private ToolsFacade toolsFacade;

    private ToolsController controller;

    @BeforeEach
    void setUp() {
        controller = new ToolsController(toolsFacade);
    }

    @Nested
    @DisplayName("GET /api/tools/weather")
    class GetWeather {

        @Test
        @DisplayName("should return weather for valid city")
        void shouldReturnWeatherForValidCity() {
            String city = "Beijing";
            String weather = "Sunny, 25°C";
            when(toolsFacade.getWeather(city)).thenReturn(weather);

            ResponseEntity<String> response = controller.getWeather(city);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(weather);
            verify(toolsFacade).getWeather(city);
        }

        @Test
        @DisplayName("should return 400 for null city")
        void shouldReturn400ForNullCity() {
            ResponseEntity<String> response = controller.getWeather(null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).contains("城市参数不能为空");
        }

        @Test
        @DisplayName("should return 400 for blank city")
        void shouldReturn400ForBlankCity() {
            ResponseEntity<String> response = controller.getWeather("   ");

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).contains("城市参数不能为空");
        }

        @Test
        @DisplayName("should return 500 when facade throws exception")
        void shouldReturn500WhenFacadeThrowsException() {
            String city = "Unknown";
            when(toolsFacade.getWeather(city)).thenThrow(new RuntimeException("API error"));

            ResponseEntity<String> response = controller.getWeather(city);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).contains("获取天气信息失败");
        }
    }

    @Nested
    @DisplayName("GET /api/tools/weather/forecast")
    class GetForecast {

        @Test
        @DisplayName("should return forecast for valid city")
        void shouldReturnForecastForValidCity() {
            String city = "Shanghai";
            String forecast = "Rainy for 3 days";
            when(toolsFacade.getForecast(city, 5)).thenReturn(forecast);

            ResponseEntity<String> response = controller.getForecast(city, 5);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(forecast);
        }

        @Test
        @DisplayName("should return forecast with null days")
        void shouldReturnForecastWithNullDays() {
            String city = "Guangzhou";
            String forecast = "Cloudy forecast";
            when(toolsFacade.getForecast(city, null)).thenReturn(forecast);

            ResponseEntity<String> response = controller.getForecast(city, null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(forecast);
        }

        @Test
        @DisplayName("should return 400 for null city")
        void shouldReturn400ForNullCity() {
            ResponseEntity<String> response = controller.getForecast(null, 3);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).contains("城市参数不能为空");
        }

        @Test
        @DisplayName("should return 400 for blank city")
        void shouldReturn400ForBlankCity() {
            ResponseEntity<String> response = controller.getForecast("", 3);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 500 when facade throws exception")
        void shouldReturn500WhenFacadeThrowsException() {
            when(toolsFacade.getForecast("ErrorCity", null)).thenThrow(new RuntimeException("API error"));

            ResponseEntity<String> response = controller.getForecast("ErrorCity", null);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).contains("获取天气预报失败");
        }
    }

    @Nested
    @DisplayName("GET /api/tools/documents/search")
    class SearchDocuments {

        @Test
        @DisplayName("should search documents with query")
        void shouldSearchDocumentsWithQuery() {
            String query = "machine learning";
            String result = "[{\"title\": \"ML Guide\"}]";
            when(toolsFacade.searchDocuments(query, null)).thenReturn(result);

            ResponseEntity<String> response = controller.searchDocuments(query, null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(result);
        }

        @Test
        @DisplayName("should search documents with docIds filter")
        void shouldSearchDocumentsWithDocIdsFilter() {
            String query = "AI";
            String docIds = "doc1,doc2,doc3";
            String result = "[{\"id\": \"doc1\"}]";
            when(toolsFacade.searchDocuments(query, List.of("doc1", "doc2", "doc3"))).thenReturn(result);

            ResponseEntity<String> response = controller.searchDocuments(query, docIds);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(result);
            verify(toolsFacade).searchDocuments(query, List.of("doc1", "doc2", "doc3"));
        }

        @Test
        @DisplayName("should return 400 for null query")
        void shouldReturn400ForNullQuery() {
            ResponseEntity<String> response = controller.searchDocuments(null, null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).contains("搜索关键词不能为空");
        }

        @Test
        @DisplayName("should return 400 for blank query")
        void shouldReturn400ForBlankQuery() {
            ResponseEntity<String> response = controller.searchDocuments("   ", null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 500 when facade throws exception")
        void shouldReturn500WhenFacadeThrowsException() {
            when(toolsFacade.searchDocuments("error", null)).thenThrow(new RuntimeException("Search error"));

            ResponseEntity<String> response = controller.searchDocuments("error", null);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).contains("搜索文档失败");
        }
    }

    @Nested
    @DisplayName("GET /api/tools/documents/list")
    class ListDocuments {

        @Test
        @DisplayName("should list all documents")
        void shouldListAllDocuments() {
            String documents = "[{\"title\": \"Doc1\"}, {\"title\": \"Doc2\"}]";
            when(toolsFacade.listDocuments()).thenReturn(documents);

            ResponseEntity<String> response = controller.listDocuments();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(documents);
        }

        @Test
        @DisplayName("should return 500 when facade throws exception")
        void shouldReturn500WhenFacadeThrowsException() {
            when(toolsFacade.listDocuments()).thenThrow(new RuntimeException("List error"));

            ResponseEntity<String> response = controller.listDocuments();

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).contains("获取文档列表失败");
        }
    }

    @Nested
    @DisplayName("POST /api/tools/chat")
    class ChatWithTools {

        @Test
        @DisplayName("should return response for valid question")
        void shouldReturnResponseForValidQuestion() {
            String question = "What's the weather in Beijing?";
            String answer = "It's sunny today!";
            when(toolsFacade.chatWithTools(question)).thenReturn(answer);

            ResponseEntity<ToolsController.ToolChatResponse> response = 
                    controller.chatWithTools(new ToolsController.ToolChatRequest(question, null));

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().answer()).isEqualTo(answer);
            assertThat(response.getBody().toolCalls()).isNull();
        }

        @Test
        @DisplayName("should pass docIds to facade")
        void shouldPassDocIdsToFacade() {
            String question = "Search in docs";
            List<String> docIds = List.of("doc1", "doc2");
            when(toolsFacade.chatWithTools(question)).thenReturn("Result");

            controller.chatWithTools(new ToolsController.ToolChatRequest(question, docIds));

            verify(toolsFacade).chatWithTools(question);
        }

        @Test
        @DisplayName("should return 400 for null request")
        void shouldReturn400ForNullRequest() {
            ResponseEntity<ToolsController.ToolChatResponse> response = controller.chatWithTools(null);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().answer()).contains("问题不能为空");
        }

        @Test
        @DisplayName("should return 400 for null question")
        void shouldReturn400ForNullQuestion() {
            ResponseEntity<ToolsController.ToolChatResponse> response = 
                    controller.chatWithTools(new ToolsController.ToolChatRequest(null, null));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody().answer()).contains("问题不能为空");
        }

        @Test
        @DisplayName("should return 400 for blank question")
        void shouldReturn400ForBlankQuestion() {
            ResponseEntity<ToolsController.ToolChatResponse> response = 
                    controller.chatWithTools(new ToolsController.ToolChatRequest("   ", null));

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should return 500 when facade throws exception")
        void shouldReturn500WhenFacadeThrowsException() {
            when(toolsFacade.chatWithTools("error question")).thenThrow(new RuntimeException("Chat error"));

            ResponseEntity<ToolsController.ToolChatResponse> response = 
                    controller.chatWithTools(new ToolsController.ToolChatRequest("error question", null));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody().answer()).contains("抱歉");
        }
    }

    @Nested
    @DisplayName("GET /api/tools/catalog")
    class ListCatalog {

        @Test
        @DisplayName("should_returnCatalog_when_facadeListsTools")
        void should_returnCatalog_when_facadeListsTools() {
            when(toolsFacade.listCatalog()).thenReturn(List.of(
                    ToolCatalogEntry.of("getWeather", "Weather", ToolSource.LOCAL),
                    ToolCatalogEntry.of("mcpTool", "MCP", ToolSource.MCP)));

            ResponseEntity<List<ToolsController.ToolCatalogResponse>> response = controller.listCatalog();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().getFirst().name()).isEqualTo("getWeather");
            assertThat(response.getBody().getFirst().source()).isEqualTo("LOCAL");
            assertThat(response.getBody().get(1).source()).isEqualTo("MCP");
        }
    }
}
