package com.ai.vision.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VisionAnalysisUseCase")
class VisionAnalysisUseCaseTest {

    @Mock
    private ChatModel visionChatModel;

    private VisionAnalysisUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VisionAnalysisUseCase(visionChatModel, new ObjectMapper());
        ReflectionTestUtils.setField(useCase, "visionModel", "qwen3.5:35b");
    }

    @Test
    @DisplayName("should parse detections from model JSON response")
    void should_parse_detections_from_model_json_response() throws Exception {
        when(visionChatModel.call(any(Prompt.class))).thenReturn(chatResponse("""
                [{"class_name":"cat","confidence":0.91,"bbox":[1,2,3,4]}]
                """));

        var response = useCase.detect(new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "bytes".getBytes()));

        assertThat(response.detections()).hasSize(1);
        assertThat(response.detections().getFirst().className()).isEqualTo("cat");
    }

    @Test
    @DisplayName("should return caption text from model response")
    void should_return_caption_text_from_model_response() throws Exception {
        when(visionChatModel.call(any(Prompt.class))).thenReturn(chatResponse("A red bicycle"));

        var response = useCase.caption(new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "bytes".getBytes()));

        assertThat(response.caption()).isEqualTo("A red bicycle");
    }

    private ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
