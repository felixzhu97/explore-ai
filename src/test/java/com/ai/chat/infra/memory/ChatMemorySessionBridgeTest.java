package com.ai.chat.infra.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

@DisplayName("ChatMemorySessionBridge")
class ChatMemorySessionBridgeTest {

  private FakeChatMemory chatMemory;
  private ChatMemorySessionBridge bridge;

  @BeforeEach
  void setUp() {
    chatMemory = new FakeChatMemory();
    bridge = new ChatMemorySessionBridge(chatMemory);
  }

  @Test
  @DisplayName("should seed memory when empty and existing messages present")
  void shouldSeedMemoryWhenEmptyAndExistingMessagesPresent() {
    List<ChatMessage> existing =
        List.of(
            ChatMessage.createUserMessage("hello"), ChatMessage.createAssistantMessage("hi there"));

    bridge.seedIfEmpty("conv-1", existing);

    assertThat(chatMemory.get("conv-1")).hasSize(2);
    assertThat(chatMemory.get("conv-1").get(0)).isInstanceOf(UserMessage.class);
    assertThat(chatMemory.get("conv-1").get(1)).isInstanceOf(AssistantMessage.class);
  }

  @Test
  @DisplayName("should skip seed when memory already has messages")
  void shouldSkipSeedWhenMemoryAlreadyHasMessages() {
    chatMemory.add("conv-1", List.of(new UserMessage("existing")));

    bridge.seedIfEmpty("conv-1", List.of(ChatMessage.createUserMessage("ignored")));

    assertThat(chatMemory.get("conv-1")).hasSize(1);
    assertThat(chatMemory.get("conv-1").get(0).getText()).isEqualTo("existing");
  }

  @Test
  @DisplayName("should skip seed when existing messages null or empty")
  void shouldSkipSeedWhenExistingMessagesNullOrEmpty() {
    bridge.seedIfEmpty("conv-1", null);
    bridge.seedIfEmpty("conv-2", List.of());

    assertThat(chatMemory.get("conv-1")).isEmpty();
    assertThat(chatMemory.get("conv-2")).isEmpty();
  }

  @Test
  @DisplayName("should sync memory messages into session and sanitize tool markup")
  void shouldSyncMemoryMessagesIntoSessionAndSanitizeToolMarkup() {
    chatMemory.add(
        "conv-1",
        List.of(
            new UserMessage("chart please"),
            new AssistantMessage("前言\n<｜DSML｜tool_calls>x</｜DSML｜tool_calls>\n后记")));

    ChatSession session = ChatSession.create("Title", "11111111-1111-1111-1111-111111111111");
    session.addUserMessage("stale");

    bridge.syncToSession("conv-1", session);

    assertThat(session.getMessageCount()).isEqualTo(2);
    assertThat(session.getMessages().get(0).getText()).isEqualTo("chart please");
    assertThat(session.getMessages().get(1).getText()).contains("前言");
    assertThat(session.getMessages().get(1).getText()).contains("后记");
    assertThat(session.getMessages().get(1).getText()).doesNotContain("DSML");
  }

  @Test
  @DisplayName("should skip sync when memory empty")
  void shouldSkipSyncWhenMemoryEmpty() {
    ChatSession session = ChatSession.create("Title", "11111111-1111-1111-1111-111111111111");
    session.addUserMessage("keep me");

    bridge.syncToSession("conv-1", session);

    assertThat(session.getMessageCount()).isEqualTo(1);
    assertThat(session.getMessages().getFirst().getText()).isEqualTo("keep me");
  }

  @Test
  @DisplayName("should clear memory for conversation")
  void shouldClearMemoryForConversation() {
    chatMemory.add("conv-1", List.of(new UserMessage("hello")));

    bridge.clear("conv-1");

    assertThat(chatMemory.get("conv-1")).isEmpty();
  }

  private static final class FakeChatMemory implements ChatMemory {
    private final Map<String, List<org.springframework.ai.chat.messages.Message>> store =
        new ConcurrentHashMap<>();

    @Override
    public void add(
        String conversationId, List<org.springframework.ai.chat.messages.Message> messages) {
      store.computeIfAbsent(conversationId, id -> new ArrayList<>()).addAll(messages);
    }

    @Override
    public List<org.springframework.ai.chat.messages.Message> get(String conversationId) {
      return List.copyOf(store.getOrDefault(conversationId, List.of()));
    }

    @Override
    public void clear(String conversationId) {
      store.remove(conversationId);
    }
  }
}
