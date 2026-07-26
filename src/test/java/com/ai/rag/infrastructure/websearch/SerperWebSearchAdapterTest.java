package com.ai.rag.infrastructure.websearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("SerperWebSearchAdapter Tests")
class SerperWebSearchAdapterTest {

    @Test
    @DisplayName("should return error message when query is blank")
    void shouldReturnErrorWhenQueryBlank() {
        assertThat(new SerperWebSearchAdapter("fake-key").searchWeb("  "))
                .isEqualTo("Please provide a valid search query.");
    }

    @Test
    @DisplayName("should return error message when query is null")
    void shouldReturnErrorWhenQueryNull() {
        assertThat(new SerperWebSearchAdapter("fake-key").searchWeb(null))
                .isEqualTo("Please provide a valid search query.");
    }

    @Test
    @DisplayName("should return error message when API key is empty")
    void shouldReturnErrorWhenApiKeyEmpty() {
        assertThat(new SerperWebSearchAdapter("").searchWeb("test query")).contains("not available");
    }

    @Test
    @DisplayName("should return no results message when response empty")
    void should_return_no_results_message_when_response_empty() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://google.serper.dev");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://google.serper.dev/search"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(new SerperWebSearchAdapter("test-key", builder.build()).searchWeb("empty"))
                .contains("No search results found");
        server.verify();
    }

    @Test
    @DisplayName("should return failure message when remote call fails")
    void should_return_failure_message_when_remote_call_fails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://google.serper.dev");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://google.serper.dev/search")).andRespond(withServerError());

        assertThat(new SerperWebSearchAdapter("test-key", builder.build()).searchWeb("fail"))
                .contains("Failed to search the web");
        server.verify();
    }

    @Test
    @DisplayName("should format answer box knowledge graph and organic results")
    void should_format_answer_box_knowledge_graph_and_organic_results() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://google.serper.dev");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://google.serper.dev/search"))
                .andRespond(withSuccess("""
                        {
                          "answerBox": {"title": "Answer title", "answer": "42", "snippet": "snippet", "link": "https://answer.example"},
                          "knowledgeGraph": {"title": "Graph title", "type": "Thing", "description": "Desc", "attributes": {"Founded": "2020"}},
                          "organic": [
                            {"title": null, "link": null, "snippet": null},
                            {"title": "Organic", "link": "https://organic.example", "snippet": "Organic snippet"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String result = new SerperWebSearchAdapter("test-key", builder.build()).searchWeb("test query");

        assertThat(result)
                .contains("Answer box", "Knowledge graph", "Organic", "https://organic.example", "Sources:");
        server.verify();
    }
}
