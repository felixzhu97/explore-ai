package com.ai.automation.infra.mail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ai.automation.domain.model.EmailMessage;
import com.ai.automation.infra.config.MailProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("ResendEmailGateway")
class ResendEmailGatewayTest {

  @Test
  @DisplayName("should post emails payload when send succeeds")
  void shouldPostEmailsPayloadWhenSendSucceeds() {
    MailProperties props = new MailProperties();
    props.setFrom("noreply@example.com");
    props.setResendApiKey("re_test_key");
    props.setResendBaseUrl("https://api.resend.com");

    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl("https://api.resend.com")
            .defaultHeader("Authorization", "Bearer re_test_key");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer re_test_key"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
                        {
                          "from": "noreply@example.com",
                          "to": ["user@example.com"],
                          "subject": "Daily brief",
                          "text": "Hello summary"
                        }
                        """))
        .andRespond(withSuccess("{\"id\":\"msg_1\"}", MediaType.APPLICATION_JSON));

    ResendEmailGateway gateway = new ResendEmailGateway(props, builder.build());
    gateway.send(new EmailMessage("user@example.com", "Daily brief", "Hello summary"));

    server.verify();
  }

  @Test
  @DisplayName("should include html when html body present")
  void shouldIncludeHtmlWhenHtmlBodyPresent() {
    MailProperties props = new MailProperties();
    props.setFrom("noreply@example.com");
    props.setResendApiKey("re_test_key");
    props.setResendBaseUrl("https://api.resend.com");

    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl("https://api.resend.com")
            .defaultHeader("Authorization", "Bearer re_test_key");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    """
                        {
                          "from": "noreply@example.com",
                          "to": ["user@example.com"],
                          "subject": "Daily brief",
                          "text": "Hello plain",
                          "html": "<p>Hello html</p>"
                        }
                        """))
        .andRespond(withSuccess("{\"id\":\"msg_2\"}", MediaType.APPLICATION_JSON));

    ResendEmailGateway gateway = new ResendEmailGateway(props, builder.build());
    gateway.send(
        new EmailMessage("user@example.com", "Daily brief", "Hello plain", "<p>Hello html</p>"));

    server.verify();
  }

  @Test
  @DisplayName("should throw when resend returns error status")
  void shouldThrowWhenResendReturnsErrorStatus() {
    MailProperties props = new MailProperties();
    props.setFrom("noreply@example.com");
    props.setResendApiKey("re_test_key");

    RestClient.Builder builder = RestClient.builder().baseUrl("https://api.resend.com");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andRespond(
            withBadRequest()
                .body("{\"message\":\"invalid\"}")
                .contentType(MediaType.APPLICATION_JSON));

    ResendEmailGateway gateway = new ResendEmailGateway(props, builder.build());

    assertThatThrownBy(() -> gateway.send(new EmailMessage("user@example.com", "Subj", "Body")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Resend API rejected");

    server.verify();
  }

  @Test
  @DisplayName("should fail fast when api key missing")
  void shouldFailFastWhenApiKeyMissing() {
    MailProperties props = new MailProperties();
    props.setResendApiKey("  ");

    assertThatThrownBy(() -> new ResendEmailGateway(props))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_MAIL_RESEND_API_KEY");
  }
}
