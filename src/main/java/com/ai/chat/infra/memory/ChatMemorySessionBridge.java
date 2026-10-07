package com.ai.chat.infra.memory;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.MessageId;
import com.ai.chat.domain.model.MessageRole;
import com.ai.chat.domain.repository.ConversationMemoryRepository;
import com.ai.common.infra.llm.ToolCallMarkupFilter;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/** Reads and seeds Spring AI ChatMemory as domain chat messages. */
@Component
@RequiredArgsConstructor
public class ChatMemorySessionBridge implements ConversationMemoryRepository {

  private final ChatMemory chatMemory;

  /** Seeds chat memory with the session's existing messages when the memory is still empty. */
  @Override
  public void loadMessagesIfEmpty(String conversationId, List<ChatMessage> existingMessages) {
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

  /** Loads the conversation's user and assistant messages; system and tool messages stay out. */
  @Override
  public List<ChatMessage> loadMessages(String conversationId) {
    return chatMemory.get(conversationId).stream()
        .filter(message -> toRole(message.getMessageType()) != null)
        .map(this::toDomainMessage)
        .toList();
  }

  /** Clears the model memory of a conversation. */
  @Override
  public void clearMessages(String conversationId) {
    chatMemory.clear(conversationId);
  }

  private Message toSpringMessage(ChatMessage message) {
    return message.isFromUser()
        ? new UserMessage(message.getText())
        : new AssistantMessage(message.getText());
  }

  private ChatMessage toDomainMessage(Message message) {
    MessageRole type = toRole(message.getMessageType());
    String text = message.getText() == null ? "" : message.getText();
    if (type == MessageRole.ASSISTANT && ToolCallMarkupFilter.looksLikeToolMarkup(text)) {
      text = ToolCallMarkupFilter.sanitize(text);
    }
    return ChatMessage.restoreMessage(MessageId.generateId(), text, type, Instant.now());
  }

  private static MessageRole toRole(MessageType type) {
    return switch (type) {
      case USER -> MessageRole.USER;
      case ASSISTANT -> MessageRole.ASSISTANT;
      default -> null;
    };
  }
}
