package com.ai.chat.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.chat.controller.dto.ChatStreamRequest;
import com.ai.chat.controller.dto.ModelsListResponse;
import com.ai.chat.controller.dto.ProviderInfoResponse;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.MessageId;
import com.ai.chat.domain.model.MessageRole;
import com.ai.chat.service.ChatService;
import com.ai.chat.service.TextProviderCatalog;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.skill.service.SkillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatStreamController {

  private final ChatService chatService;
  private final TextProviderCatalog providerCatalog;
  private final SkillService skillService;
  private final OwnerContext ownerContext;

  /** Lists the chat providers and whether they are available. */
  @GetMapping("/providers")
  public List<ProviderInfoResponse> listProviders() {
    return providerCatalog.listProviders();
  }

  /** Lists the chat models of a provider. */
  @GetMapping("/models")
  public ModelsListResponse listModels(@RequestParam(required = false) String provider) {
    var models = providerCatalog.listModels(provider);
    String resolvedProvider =
        provider == null || provider.isBlank() ? "openai" : provider.toLowerCase();
    return ModelsListResponse.of(resolvedProvider, models);
  }

  /** Streams the chat reply as server-sent events. */
  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> streamChat(
      @Valid @RequestBody ChatStreamRequest request, HttpServletRequest httpRequest) {
    TextChatOptions options = buildChatOptions(request, httpRequest);
    String ownerKey = ownerContext.requireValue(httpRequest);

    if (request.sessionId() != null && !request.sessionId().isBlank()) {
      String userMessage = extractLastUserMessage(request.messages());
      if (userMessage == null || userMessage.isBlank()) {
        return Flux.error(
            new IllegalArgumentException("User message is required when sessionId is provided"));
      }
      return chatService.streamChatWithSession(request.sessionId(), userMessage, options, ownerKey);
    }

    List<ChatMessage> messages =
        request.messages().stream()
            .map(
                dto ->
                    ChatMessage.restore(
                        MessageId.generate(), dto.content(), dto.role(), Instant.now()))
            .toList();
    return chatService.streamChat(messages, options, ownerKey);
  }

  private TextChatOptions buildChatOptions(
      ChatStreamRequest request, HttpServletRequest httpRequest) {
    TextChatOptions baseOptions =
        TextChatOptions.of(request.provider(), request.model(), request.toolsEnabled());
    List<String> skillIds = request.skillIds();
    if (skillIds == null || skillIds.isEmpty()) {
      return baseOptions;
    }
    return skillService
        .activeSkillsPrompt(ownerContext.requireValue(httpRequest), skillIds)
        .map(baseOptions::withSkillSystemPrompt)
        .orElse(baseOptions);
  }

  private String extractLastUserMessage(List<ChatStreamRequest.Message> messages) {
    for (int i = messages.size() - 1; i >= 0; i--) {
      ChatStreamRequest.Message message = messages.get(i);
      if (message.role() == MessageRole.USER) {
        return message.content();
      }
    }
    return null;
  }
}
