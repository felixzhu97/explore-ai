package com.ai.chat.infra.memory;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.chat.domain.vo.MessageId;
import com.ai.common.infra.llm.ToolCallMarkupFilter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/** Synchronizes Spring AI ChatMemory with domain ChatSession aggregates. */
@Component
public class ChatMemorySessionBridge implements ConversationMemoryRepository {

  private final ChatMemory chatMemory;

  /** Documentation. */
  public ChatMemorySessionBridge(ChatMemory chatMemory) {
    this.chatMemory = chatMemory;
  }

  /** Documentation. */
  public void seedIfEmpty(String conversationId, List<ChatMessage> existingMessages) {
    if (existingMessages == null || existingMessages.isEmpty()) {
      return;
    }
    List<Message> memoryMessages = chatMemory.get(conversationId);
    if (!memoryMessages.isEmpty()) {
      return;
    }
    List<Message> toSeed = existingMessages.stream().map(this::toSpringMessage).toList();
    chatMemory.add(conversationId, toSeed);
  }

  /** Documentation. */
  public void syncToSession(String conversationId, ChatSession session) {
    List<Message> memoryMessages = chatMemory.get(conversationId);
    if (memoryMessages.isEmpty()) {
      return;
    }
    List<ChatMessage> domainMessages = new ArrayList<>();
    for (Message message : memoryMessages) {
      domainMessages.add(toDomainMessage(message));
    }
    session.replaceMessages(domainMessages);
  }

  /** Documentation. */
  public void clear(String conversationId) {
    chatMemory.clear(conversationId);
  }

  private Message toSpringMessage(ChatMessage message) {
    return message.isFromUser()
        ? new UserMessage(message.getText())
        : new AssistantMessage(message.getText());
  }

  private ChatMessage toDomainMessage(Message message) {
    String role = message.getMessageType().getValue();
    String text = message.getText() == null ? "" : message.getText();
    if (message.getMessageType() == MessageType.ASSISTANT
        && ToolCallMarkupFilter.looksLikeToolMarkup(text)) {
      text = ToolCallMarkupFilter.sanitize(text);
    }
    return ChatMessage.of(MessageId.generate(), text, role, Instant.now());
  }
}
