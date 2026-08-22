package com.ai.tools.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.testsupport.SliceWebMvcTest;
import com.ai.tools.service.usecase.ToolsFacade;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = ToolsController.class)
@DisplayName("ToolsController")
class ToolsControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private ToolsFacade toolsFacade;

  @Nested
  @DisplayName("GET /api/tools/weather")
  class GetWeather {

    @Test
    @DisplayName("should return weather for valid city")
    void shouldReturnWeatherForValidCity() {
      String city = "Beijing";
      String weather = "Sunny, 25°C";
      when(toolsFacade.getWeather(city)).thenReturn(weather);

      assertThat(mvc.get().uri("/api/tools/weather").param("city", city))
          .hasStatusOk()
          .hasBodyTextEqualTo(weather);
      verify(toolsFacade).getWeather(city);
    }

    @Test
    @DisplayName("should return 400 for null city")
    void shouldReturn400ForNullCity() {
      assertThat(mvc.get().uri("/api/tools/weather"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyText()
          .asString()
          .contains("城市参数不能为空");
    }

    @Test
    @DisplayName("should return 400 for blank city")
    void shouldReturn400ForBlankCity() {
      assertThat(mvc.get().uri("/api/tools/weather").param("city", "   "))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyText()
          .asString()
          .contains("城市参数不能为空");
    }

    @Test
    @DisplayName("should return 500 when facade throws exception")
    void shouldReturn500WhenFacadeThrowsException() {
      String city = "Unknown";
      when(toolsFacade.getWeather(city)).thenThrow(new RuntimeException("API error"));

      assertThat(mvc.get().uri("/api/tools/weather").param("city", city))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("获取天气信息失败");
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
      when(toolsFacade.getForecast(city, null)).thenReturn(forecast);

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
          .contains("城市参数不能为空");
    }

    @Test
    @DisplayName("should return 400 for blank city")
    void shouldReturn400ForBlankCity() {
      assertThat(mvc.get().uri("/api/tools/weather/forecast").param("city", "").param("days", "3"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 500 when facade throws exception")
    void shouldReturn500WhenFacadeThrowsException() {
      when(toolsFacade.getForecast("ErrorCity", null)).thenThrow(new RuntimeException("API error"));

      assertThat(mvc.get().uri("/api/tools/weather/forecast").param("city", "ErrorCity"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("获取天气预报失败");
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

      assertThat(mvc.get().uri("/api/tools/documents/search").param("query", query))
          .hasStatusOk()
          .hasBodyTextEqualTo(result);
    }

    @Test
    @DisplayName("should search documents with docIds filter")
    void shouldSearchDocumentsWithDocIdsFilter() {
      String query = "AI";
      String docIds = "doc1,doc2,doc3";
      String result = "[{\"id\": \"doc1\"}]";
      when(toolsFacade.searchDocuments(query, List.of("doc1", "doc2", "doc3"))).thenReturn(result);

      assertThat(
              mvc.get()
                  .uri("/api/tools/documents/search")
                  .param("query", query)
                  .param("docIds", docIds))
          .hasStatusOk()
          .hasBodyTextEqualTo(result);
      verify(toolsFacade).searchDocuments(query, List.of("doc1", "doc2", "doc3"));
    }

    @Test
    @DisplayName("should return 400 for null query")
    void shouldReturn400ForNullQuery() {
      assertThat(mvc.get().uri("/api/tools/documents/search"))
          .hasStatus(HttpStatus.BAD_REQUEST)
          .bodyText()
          .asString()
          .contains("搜索关键词不能为空");
    }

    @Test
    @DisplayName("should return 400 for blank query")
    void shouldReturn400ForBlankQuery() {
      assertThat(mvc.get().uri("/api/tools/documents/search").param("query", "   "))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return 500 when facade throws exception")
    void shouldReturn500WhenFacadeThrowsException() {
      when(toolsFacade.searchDocuments("error", null))
          .thenThrow(new RuntimeException("Search error"));

      assertThat(mvc.get().uri("/api/tools/documents/search").param("query", "error"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("搜索文档失败");
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

      assertThat(mvc.get().uri("/api/tools/documents/list"))
          .hasStatusOk()
          .hasBodyTextEqualTo(documents);
    }

    @Test
    @DisplayName("should return 500 when facade throws exception")
    void shouldReturn500WhenFacadeThrowsException() {
      when(toolsFacade.listDocuments()).thenThrow(new RuntimeException("List error"));

      assertThat(mvc.get().uri("/api/tools/documents/list"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyText()
          .asString()
          .contains("获取文档列表失败");
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
    @DisplayName("should pass docIds to facade")
    void shouldPassDocIdsToFacade() {
      String question = "Search in docs";
      when(toolsFacade.chatWithTools(question)).thenReturn("Result");

      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"Search in docs\",\"docIds\":[\"doc1\",\"doc2\"]}"))
          .hasStatusOk();

      verify(toolsFacade).chatWithTools(question);
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
          .extractingPath("$.answer")
          .asString()
          .contains("问题不能为空");
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
          .extractingPath("$.answer")
          .asString()
          .contains("问题不能为空");
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
    @DisplayName("should return 500 when facade throws exception")
    void shouldReturn500WhenFacadeThrowsException() {
      when(toolsFacade.chatWithTools("error question"))
          .thenThrow(new RuntimeException("Chat error"));

      assertThat(
              mvc.post()
                  .uri("/api/tools/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"error question\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
          .bodyJson()
          .extractingPath("$.answer")
          .asString()
          .contains("抱歉");
    }
  }
}
