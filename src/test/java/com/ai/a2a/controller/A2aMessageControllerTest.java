package com.ai.a2a.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.a2a.domain.model.A2aTask;
import com.ai.a2a.service.A2aService;
import com.ai.testsupport.AbstractOwnerScopedControllerTest;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SliceWebMvcTest(controllers = A2aMessageController.class)
@DisplayName("A2aMessageController")
class A2aMessageControllerTest extends AbstractOwnerScopedControllerTest {

  @MockitoBean private A2aService a2aService;

  @Test
  @DisplayName("should send message and return completed task")
  void shouldSendMessage() {
    A2aTask task = A2aTask.createSubmitted("task-1");
    task.markWorking();
    task.completeWithText("answer");
    when(a2aService.sendMessage(any(), eq(ownerKey()), anyString())).thenReturn(task);

    assertThat(
            mvc.post()
                .uri("/a2a/v1/message:send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "message": {
                        "messageId": "msg-1",
                        "role": "user",
                        "parts": [{"text": "hello"}]
                      }
                    }
                    """)
                .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.status.state")
        .isEqualTo("completed");
  }
}
