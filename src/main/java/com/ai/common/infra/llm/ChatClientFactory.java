package com.ai.common.infra.llm;

import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.DocumentSearchTool;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.common.service.llm.WebSearchTool;
import com.ai.tools.infra.tools.DateTimeTools;
import com.ai.tools.infra.tools.WeatherTools;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Builds chat clients per profile with memory, system prompt, and tool advisors. */
@Component
public class ChatClientFactory implements ChatClientProvider {

  private final ChatModelResolver chatModelResolver;
  private final ChatMemory chatMemory;
  private final PromptTemplates promptTemplates;
  private final ObjectProvider<ToolCallback[]> mcpToolCallbacks;
  private final boolean toolSearchEnabled;
  private final ToolCallback[] localToolCallbacks;
  private final RegexToolIndex toolSearchIndex;

  public ChatClientFactory(
      ChatModelResolver chatModelResolver,
      ChatMemory chatMemory,
      PromptTemplates promptTemplates,
      WeatherTools weatherTools,
      DocumentSearchTool documentSearchTool,
      WebSearchTool webSearchTool,
      DateTimeTools dateTimeTool,
      ObjectProvider<ToolCallback[]> mcpToolCallbacks,
      @Value("${app.ai.tool-search.enabled:false}") boolean toolSearchEnabled) {
    this.chatModelResolver = chatModelResolver;
    this.chatMemory = chatMemory;
    this.promptTemplates = promptTemplates;
    this.mcpToolCallbacks = mcpToolCallbacks;
    this.toolSearchEnabled = toolSearchEnabled;
    this.toolSearchIndex = new RegexToolIndex();
    this.localToolCallbacks =
        MethodToolCallbackProvider.builder()
            .toolObjects(weatherTools, documentSearchTool, webSearchTool, dateTimeTool)
            .build()
            .getToolCallbacks();
  }

  @Override
  public ChatClient create(TextChatOptions options) {
    return create(options, null);
  }

  @Override
  public ChatClient create(TextChatOptions options, String conversationId) {
    return create(options, ChatClientProfile.MEMORY_TOOLS, conversationId);
  }

  @Override
  public ChatClient create(
      TextChatOptions options, ChatClientProfile profile, String conversationId) {
    boolean withMemory =
        profile == ChatClientProfile.MEMORY_TOOLS || profile == ChatClientProfile.MEMORY;
    boolean withDefaults = profile != ChatClientProfile.BARE;
    boolean withTools =
        (profile == ChatClientProfile.MEMORY_TOOLS || profile == ChatClientProfile.TOOLS)
            && options.toolsEnabled();
    return buildClient(options, withMemory, withDefaults, withTools, conversationId);
  }

  @Override
  public ChatClient createStateless(TextChatOptions options) {
    return create(options, ChatClientProfile.TOOLS, ToolEventChannel.getCurrentSessionId());
  }

  @Override
  public ChatClient createBareStateless(TextChatOptions options) {
    return create(options, ChatClientProfile.BARE, null);
  }

  private ChatClient buildClient(
      TextChatOptions options,
      boolean withMemory,
      boolean withDefaults,
      boolean withTools,
      String channelId) {
    final ResolvedChatModel resolved = chatModelResolver.resolve(options);
    List<Advisor> advisors = new ArrayList<>();
    if (withMemory) {
      advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
    }
    if (withTools) {
      // ToolAdvisor in the chain skips ChatClient's auto-registered ToolCallingAdvisor.
      advisors.add(createToolCallingAdvisor());
    }

    ChatClient.Builder builder =
        ChatClient.builder(resolved.chatModel())
            .defaultOptions(resolved.optionsBuilder())
            .defaultAdvisors(advisors);

    if (withDefaults) {
      String systemPrompt = promptTemplates.getDefaultSystemPrompt();
      if (options.skillSystemPrompt() != null && !options.skillSystemPrompt().isBlank()) {
        systemPrompt = systemPrompt + "\n\n" + options.skillSystemPrompt();
      }
      builder.defaultSystem(systemPrompt);
      if (withTools) {
        ToolCallback[] callbacks = createNotifyingCallbacks(channelId);
        if (callbacks.length > 0) {
          builder.defaultToolCallbacks(callbacks);
        }
      }
    }

    return builder.build();
  }

  /**
   * When {@code app.ai.tool-search.enabled=true}, expose tools via {@link
   * ToolSearchToolCallingAdvisor} so only search-matched tools enter the model context. Otherwise
   * keep {@link ToolCallLoopGuardAdvisor}.
   */
  Advisor createToolCallingAdvisor() {
    ToolCallingManager manager = ToolCallingManager.builder().build();
    if (toolSearchEnabled) {
      return ToolSearchToolCallingAdvisor.builder()
          .toolCallingManager(manager)
          .toolIndex(toolSearchIndex)
          .build();
    }
    return ToolCallLoopGuardAdvisor.builder().toolCallingManager(manager).build();
  }

  /** Tells whether tool search is on. */
  boolean isToolSearchEnabled() {
    return toolSearchEnabled;
  }

  private ToolCallback[] createNotifyingCallbacks(String channelId) {
    String id = channelId == null ? "" : channelId;
    List<ToolCallback> callbacks = new ArrayList<>();
    Set<String> names = new HashSet<>();
    for (ToolCallback callback : localToolCallbacks) {
      String name = callback.getToolDefinition().name();
      if (names.add(name)) {
        callbacks.add(new NotifyingToolCallback(callback, id));
      }
    }
    // Prefer local @Tool over MCP when names collide (DeepSeek/Spring AI reject duplicates).
    ToolCallback[] mcp = mcpToolCallbacks.getIfAvailable();
    if (mcp != null) {
      for (ToolCallback callback : mcp) {
        String name = callback.getToolDefinition().name();
        if (names.add(name)) {
          callbacks.add(new NotifyingToolCallback(callback, id));
        }
      }
    }
    return callbacks.toArray(ToolCallback[]::new);
  }
}
