package com.ai.rag.infrastructure.websearch;

import com.ai.common.infrastructure.llm.ToolEventChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("SerperWebSearchAdapter")
class SerperWebSearchAdapterSourcesTest {

    @AfterEach
    void tearDown() {
        ToolEventChannel.close();
    }

    @Test
    @DisplayName("should_publish_sources_event_when_search_succeeds")
    void should_publish_sources_event_when_search_succeeds() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://google.serper.dev");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://google.serper.dev/search"))
                .andRespond(withSuccess("""
                        {
                          "organic": [
                            {
                              "title": "Example",
                              "link": "https://example.com",
                              "snippet": "Example snippet"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var sink = ToolEventChannel.open();
        List<String> events = new ArrayList<>();
        sink.asFlux().subscribe(events::add);

        SerperWebSearchAdapter adapter = new SerperWebSearchAdapter("test-key", builder.build());
        String result = adapter.searchWeb("weather beijing");

        assertThat(result).contains("Example").contains("https://example.com");
        assertThat(events).anySatisfy(event ->
                assertThat(event)
                        .contains("\"type\":\"sources\"")
                        .contains("weather beijing")
                        .contains("https://example.com"));
        server.verify();
        ToolEventChannel.close();
    }
}
