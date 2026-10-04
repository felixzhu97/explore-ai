package com.ai.chat.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.chat.controller.dto.ChatStreamRequest;
import com.ai.chat.controller.dto.ModelsListResponse;
import com.ai.chat.controller.dto.ProviderInfoResponse;
import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.service.ChatService;
import com.ai.chat.service.TextProviderCatalog;
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

@RestController
@RequestMapping("/api/chat")
public class ChatStreamController {

  private final OwnerContext ownerContext;

  private static final Logger log = LoggerFactory.getLogger(ChatStreamController.class);

  private final ChatService chatService;
  private final TextProviderCatalog providerCatalog;
  private final SkillRepository skillRepository;

  public ChatStreamController(
      ChatService chatService,
      TextProviderCatalog providerCatalog,
      SkillRepository skillRepository,
      OwnerContext ownerContext) {
    this.ownerContext = ownerContext;
    this.chatService = chatService;
    this.providerCatalog = providerCatalog;
    this.skillRepository = skillRepository;
  }

  @GetMapping("/providers")
  public List<ProviderInfoResponse> listProviders() {
    return providerCatalog.listProviders();
  }

  @GetMapping("/models")
  public ModelsListResponse listModels(@RequestParam(required = false) String provider) {
    var models = providerCatalog.listModels(provider);
    String resolvedProvider =
        provider == null || provider.isBlank() ? "openai" : provider.toLowerCase();
    return ModelsListResponse.of(resolvedProvider, models);
  }

  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> chatStream(
      @RequestBody ChatStreamRequest request, HttpServletRequest httpRequest) {
    TextChatOptions options = buildChatOptions(request, httpRequest);
    String ownerKey = ownerContext.requireValue(httpRequest);

    if (request.sessionId() != null && !request.sessionId().isBlank()) {
      String userMessage = extractLastUserMessage(request.messages());
      if (userMessage == null || userMessage.isBlank()) {
        return Flux.error(
            new IllegalArgumentException("User message is required when sessionId is provided"));
      }
      return chatService.chatStreamWithSession(request.sessionId(), userMessage, options, ownerKey);
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
    return chatService.chatStream(messages, options, ownerKey);
  }

  private TextChatOptions buildChatOptions(
      ChatStreamRequest request, HttpServletRequest httpRequest) {
    TextChatOptions baseOptions =
        TextChatOptions.of(request.provider(), request.model(), request.toolsEnabled());
    List<String> skillIds = request.skillIds();
    if (skillIds == null || skillIds.isEmpty()) {
      return baseOptions;
    }

    String ownerKey = ownerContext.requireValue(httpRequest);
    List<SkillId> parsedSkillIds = parseSkillIds(skillIds);
    if (parsedSkillIds.isEmpty()) {
      return baseOptions;
    }

    List<Skill> skills = skillRepository.findEnabledByOwnerKeyAndIds(ownerKey, parsedSkillIds);
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

  private String extractLastUserMessage(List<ChatStreamRequest.Message> messages) {
    if (messages == null || messages.isEmpty()) {
      return null;
    }
    for (int i = messages.size() - 1; i >= 0; i--) {
      ChatStreamRequest.Message message = messages.get(i);
      if ("user".equalsIgnoreCase(message.role())) {
        return message.content();
      }
    }
    return null;
  }
}
