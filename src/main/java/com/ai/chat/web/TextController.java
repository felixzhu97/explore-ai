package com.ai.chat.web;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.vo.MessageId;
import com.ai.chat.web.dto.ChatStreamRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/text")
public class TextController {

    private final ChatUseCase chatUseCase;

    public TextController(ChatUseCase chatUseCase) {
        this.chatUseCase = chatUseCase;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatStreamRequest request) {
        List<ChatMessage> messages = request.messages().stream()
                .map(dto -> ChatMessage.of(
                        MessageId.generate(),
                        dto.content(),
                        dto.role(),
                        Instant.now()))
                .toList();
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            return chatUseCase.chatStreamWithSession(request.sessionId(), messages);
        }
        return chatUseCase.chatStreamWithSession(messages);
    }
}
