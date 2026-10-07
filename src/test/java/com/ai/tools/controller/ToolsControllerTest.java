package com.ai.tools.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.testsupport.AbstractOwnerScopedControllerTest;
import com.ai.testsupport.OwnerKeyFixtures;
import com.ai.testsupport.SliceWebMvcTest;
import com.ai.tools.service.ToolService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SliceWebMvcTest(controllers = ToolsController.class)
@DisplayName("ToolsController")
class ToolsControllerTest extends AbstractOwnerScopedControllerTest {

  private static final OwnerKey OWNER = OwnerKey.parse(OwnerKeyFixtures.CLIENT_FULL_KEY);

  @MockitoBean private ToolService toolService;

  @Nested
  @DisplayName("GET /api/tools/weather")
  class GetWeather {

    @Test
    @DisplayName("should return weather for valid city")
    void shouldReturnWeatherForValidCity() {
      String city = "Beijing";
      String weather = "Sunny, 25°C";
      when(toolService.lookupWeather(city)).thenReturn(weather);

      assertThat(mvc.get().uri("/api/tools/weather").param("city", city))
          .hasStatusOk()
          .hasBodyTextEqualTo(weather);
      verify(toolService).lookupWeather(city);
    }

    @Test
    @DisplayName("should return 400 for null city")
    void shouldReturn400ForNullCity() {
      assertThat(mvc.get().uri("/api/tools/weather"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyText()
          .asString()
          .contains("Required parameter 'city' is missing");
    }

    @Test
    @DisplayName("should return 400 for blank city")
    void shouldReturn400ForBlankCity() {
      assertThat(mvc.get().uri("/api/tools/weather").param("city", "   "))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyText()
          .asString()
          .contains("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() {
      String city = "Unknown";
      when(toolService.lookupWeather(city)).thenThrow(new RuntimeException("API error"));

      assertThat(mvc.get().uri("/api/tools/weather").param("city", city))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("INTERNAL_ERROR");
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
      when(toolService.lookupForecast(city, 5)).thenReturn(forecast);

      assertThat(
              mvc.get().uri("/api/tools/weather/forecast").param("city", city).param("days", "5"))
          .hasStatusOk()
          .hasBodyTextEqualTo(forecast);
    }

    @Test
    @DisplayName("should return forecast with null days")
    void shouldReturnForecastWithNullDays() {
      String city = "Guangzhou";
      String forecast = "Cloudy forecast";
      when(toolService.lookupForecast(city, null)).thenReturn(forecast);

      assertThat(mvc.get().uri("/api/tools/weather/forecast").param("city", city))
          .hasStatusOk()
          .hasBodyTextEqualTo(forecast);
    }

    @Test
    @DisplayName("should return 400 for null city")
    void shouldReturn400ForNullCity() {
      assertThat(mvc.get().uri("/api/tools/weather/forecast").param("days", "3"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyText()
          .asString()
          .contains("Required parameter 'city' is missing");
    }

    @Test
    @DisplayName("should return 400 for blank city")
    void shouldReturn400ForBlankCity() {
      assertThat(mvc.get().uri("/api/tools/weather/forecast").param("city", "").param("days", "3"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() {
      when(toolService.lookupForecast("ErrorCity", null))
          .thenThrow(new RuntimeException("API error"));

      assertThat(mvc.get().uri("/api/tools/weather/forecast").param("city", "ErrorCity"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("INTERNAL_ERROR");
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
      when(toolService.searchDocuments(query, null)).thenReturn(result);

      assertThat(mvc.get().uri("/api/tools/documents/search").param("query", query))
          .hasStatusOk()
          .hasBodyTextEqualTo(result);
    }

    @Test
    @DisplayName("should search documents with documentIds filter")
    void shouldSearchDocumentsWithDocIdsFilter() {
      String query = "AI";
      String documentIds = "doc1,doc2,doc3";
      String result = "[{\"id\": \"doc1\"}]";
      when(toolService.searchDocuments(query, List.of("doc1", "doc2", "doc3"))).thenReturn(result);

      assertThat(
              mvc.get()
                  .uri("/api/tools/documents/search")
                  .param("query", query)
                  .param("documentIds", documentIds))
          .hasStatusOk()
          .hasBodyTextEqualTo(result);
      verify(toolService).searchDocuments(query, List.of("doc1", "doc2", "doc3"));
    }

    @Test
    @DisplayName("should return 400 for null query")
    void shouldReturn400ForNullQuery() {
      assertThat(mvc.get().uri("/api/tools/documents/search"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyText()
          .asString()
          .contains("Required parameter 'query' is missing");
    }

    @Test
    @DisplayName("should return 400 for blank query")
    void shouldReturn400ForBlankQuery() {
      assertThat(mvc.get().uri("/api/tools/documents/search").param("query", "   "))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() {
      when(toolService.searchDocuments("error", null))
          .thenThrow(new RuntimeException("Search error"));

      assertThat(mvc.get().uri("/api/tools/documents/search").param("query", "error"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("INTERNAL_ERROR");
    }
  }

  @Nested
  @DisplayName("GET /api/tools/documents")
  class ListDocuments {

    @Test
    @DisplayName("should list all documents")
    void shouldListAllDocuments() {
      String documents = "[{\"title\": \"Doc1\"}, {\"title\": \"Doc2\"}]";
      when(toolService.listDocuments()).thenReturn(documents);

      assertThat(mvc.get().uri("/api/tools/documents")).hasStatusOk().hasBodyTextEqualTo(documents);
    }

    @Test
    @DisplayName("should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() {
      when(toolService.listDocuments()).thenThrow(new RuntimeException("List error"));

      assertThat(mvc.get().uri("/api/tools/documents"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("INTERNAL_ERROR");
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
      when(toolService.chatWithTools(question, OWNER)).thenReturn(answer);

      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"" + question + "\"}"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.answer")
          .asString()
          .isEqualTo(answer);
    }

    @Test
    @DisplayName("should pass documentIds to service")
    void shouldPassDocIdsToService() {
      String question = "Search in docs";
      when(toolService.chatWithTools(question, OWNER)).thenReturn("Result");

      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"Search in docs\",\"documentIds\":[\"doc1\",\"doc2\"]}"))
          .hasStatusOk();

      verify(toolService).chatWithTools(question, OWNER);
    }

    @Test
    @DisplayName("should return 400 for empty request body")
    void shouldReturn400ForEmptyRequestBody() {
      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.errorCode")
          .asString()
          .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("should return 400 for null question")
    void shouldReturn400ForNullQuestion() {
      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":null}"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyJson()
          .extractingPath("$.errorCode")
          .asString()
          .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("should return 400 for blank question")
    void shouldReturn400ForBlankQuestion() {
      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"   \"}"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() {
      when(toolService.chatWithTools("error question", OWNER))
          .thenThrow(new RuntimeException("Chat error"));

      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"error question\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyJson()
          .extractingPath("$.errorCode")
          .asString()
          .isEqualTo("INTERNAL_ERROR");
    }
  }
}
