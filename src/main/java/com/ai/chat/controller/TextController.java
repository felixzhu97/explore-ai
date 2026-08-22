package com.ai.chat.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.chat.controller.dto.ChatStreamRequest;
import com.ai.chat.controller.dto.ModelsListResponse;
import com.ai.chat.controller.dto.ProviderInfoResponse;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.service.usecase.ChatUseCase;
import com.ai.chat.service.usecase.TextProviderCatalog;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import com.ai.skill.service.SkillSystemPromptBuilder;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** Documentation. */
@RestController
@RequestMapping("/api/text")
public class TextController {

  private final OwnerContext ownerContext;

  private static final Logger log = LoggerFactory.getLogger(TextController.class);

  private final ChatUseCase chatUseCase;
  private final TextProviderCatalog providerCatalog;
  private final SkillRepository skillRepository;

  /** Documentation. */
  public TextController(
      ChatUseCase chatUseCase,
      TextProviderCatalog providerCatalog,
      SkillRepository skillRepository,
      OwnerContext ownerContext) {
    this.ownerContext = ownerContext;
    this.chatUseCase = chatUseCase;
    this.providerCatalog = providerCatalog;
    this.skillRepository = skillRepository;
  }

  /** Documentation. */
  @GetMapping("/providers")
  public List<ProviderInfoResponse> listProviders() {
    return providerCatalog.listProviders();
  }

  /** Documentation. */
  @GetMapping("/models")
  public ModelsListResponse listModels(@RequestParam(required = false) String provider) {
    var models = providerCatalog.listModels(provider);
    String resolvedProvider =
        provider == null || provider.isBlank() ? "openai" : provider.toLowerCase();
    return ModelsListResponse.of(resolvedProvider, models);
  }

  /** Documentation. */
  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> chatStream(
      @RequestBody ChatStreamRequest request, HttpServletRequest httpRequest) {
    TextChatOptions options = buildChatOptions(request, httpRequest);

    if (request.sessionId() != null && !request.sessionId().isBlank()) {
      String userMessage = extractLastUserMessage(request.messages());
      if (userMessage == null || userMessage.isBlank()) {
        return Flux.error(
            new IllegalArgumentException("User message is required when sessionId is provided"));
      }
      String clientId = ownerContext.requireValue(httpRequest);
      return chatUseCase.chatStreamWithSession(request.sessionId(), userMessage, options, clientId);
    }

    List<ChatMessage> messages =
        request.messages().stream()
            .map(
                dto ->
                    ChatMessage.of(
                        com.ai.chat.domain.vo.MessageId.generate(),
                        dto.content(),
                        dto.role(),
                        Instant.now()))
            .toList();
    return chatUseCase.chatStream(messages, options);
  }

  private TextChatOptions buildChatOptions(
      ChatStreamRequest request, HttpServletRequest httpRequest) {
    TextChatOptions baseOptions =
        TextChatOptions.of(request.provider(), request.model(), request.toolsEnabled());
    List<String> skillIds = request.skillIds();
    if (skillIds == null || skillIds.isEmpty()) {
      return baseOptions;
    }

    String clientId = ownerContext.requireValue(httpRequest);
    List<SkillId> parsedSkillIds = parseSkillIds(skillIds);
    if (parsedSkillIds.isEmpty()) {
      return baseOptions;
    }

    List<Skill> skills = skillRepository.findEnabledByClientIdAndIds(clientId, parsedSkillIds);
    if (skills.size() < parsedSkillIds.size()) {
      log.debug(
          "Ignored unknown or disabled skill ids: requested={}, resolved={}",
          parsedSkillIds.size(),
          skills.size());
    }

    String skillSystemPrompt = SkillSystemPromptBuilder.build(skills);
    if (skillSystemPrompt == null || skillSystemPrompt.isBlank()) {
      return baseOptions;
    }
    return baseOptions.withSkillSystemPrompt(skillSystemPrompt);
  }

  private List<SkillId> parseSkillIds(List<String> skillIds) {
    List<SkillId> parsed = new ArrayList<>();
    for (String skillId : skillIds) {
      if (skillId == null || skillId.isBlank()) {
        continue;
      }
      try {
        parsed.add(SkillId.of(skillId.trim()));
      } catch (IllegalArgumentException ignored) {
        log.debug("Ignoring invalid skill id: {}", skillId);
      }
    }
    return parsed;
  }

  private String extractLastUserMessage(List<ChatStreamRequest.ChatMessageDto> messages) {
    if (messages == null || messages.isEmpty()) {
      return null;
    }
    for (int i = messages.size() - 1; i >= 0; i--) {
      ChatStreamRequest.ChatMessageDto message = messages.get(i);
      if ("user".equalsIgnoreCase(message.role())) {
        return message.content();
      }
    }
    return null;
  }
}
