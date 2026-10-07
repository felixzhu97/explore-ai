package com.ai.chat.infra.memory;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatMessageType;
import com.ai.chat.domain.model.MessageId;
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

  /** Loads the conversation's user and assistant messages; system and tool messages stay out. */
  @Override
  public List<ChatMessage> load(String conversationId) {
    return chatMemory.get(conversationId).stream()
        .filter(message -> toMessageType(message.getMessageType()) != null)
        .map(this::toDomainMessage)
        .toList();
  }

  /** Clears the model memory of a conversation. */
  @Override
  public void clear(String conversationId) {
    chatMemory.clear(conversationId);
  }

  private Message toSpringMessage(ChatMessage message) {
    return message.isFromUser()
        ? new UserMessage(message.getText())
        : new AssistantMessage(message.getText());
  }

  private ChatMessage toDomainMessage(Message message) {
    ChatMessageType type = toMessageType(message.getMessageType());
    String text = message.getText() == null ? "" : message.getText();
    if (type == ChatMessageType.ASSISTANT && ToolCallMarkupFilter.looksLikeToolMarkup(text)) {
      text = ToolCallMarkupFilter.sanitize(text);
    }
    return ChatMessage.restore(MessageId.generate(), text, type, Instant.now());
  }

  private static ChatMessageType toMessageType(MessageType type) {
    return switch (type) {
      case USER -> ChatMessageType.USER;
      case ASSISTANT -> ChatMessageType.ASSISTANT;
      default -> null;
    };
  }
}
