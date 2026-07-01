package com.ai.chat.web;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.web.dto.ChatStreamRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/text")
public class TextController {

    private final ChatUseCase chatUseCase;

    public TextController(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatStreamRequest request) {
        List<ChatMessage> messages = request.messages().stream()
                .map(dto -> ChatMessage.of(
                        com.ai.chat.domain.vo.MessageId.generate(),
                        dto.content(),
                        dto.role(),
                        java.time.Instant.now()))
                .toList();
        return chatUseCase.chatStream(messages);
    }
}
