package com.ai.pipeline.infra.llm;

import com.ai.common.domain.repository.DateTimeTool;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infra.llm.ToolCallMarkupFilter;
import com.ai.common.infra.skills.AgentSkillsRuntime;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.service.WorkerAgentInvoker;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Documentation. */
@Component
public class SpringAiWorkerAgentInvoker implements WorkerAgentInvoker {

  private final ChatClientProvider chatClientProvider;
  private final DocumentSearchTool documentSearchTool;
  private final WebSearchTool webSearchTool;
  private final WeatherTool weatherTool;
  private final DateTimeTool dateTimeTool;
  private final AgentSkillsRuntime agentSkillsRuntime;

  /** Documentation. */
  public SpringAiWorkerAgentInvoker(
      ChatClientProvider chatClientProvider,
      DocumentSearchTool documentSearchTool,
      WebSearchTool webSearchTool,
      WeatherTool weatherTool,
      DateTimeTool dateTimeTool,
      AgentSkillsRuntime agentSkillsRuntime) {
    this.chatClientProvider = chatClientProvider;
    this.documentSearchTool = documentSearchTool;
    this.webSearchTool = webSearchTool;
    this.weatherTool = weatherTool;
    this.dateTimeTool = dateTimeTool;
    this.agentSkillsRuntime = agentSkillsRuntime;
  }

  @Override
  public Flux<String> invokeStream(AgentDefinition agent, String task) {
    // Tool-calling models often stream tool markup as plain text; block for tool loop.
    if (usesTools(agent)) {
      return Mono.fromCallable(() -> invoke(agent, task))
          .subscribeOn(Schedulers.boundedElastic())
          .flatMapMany(
              answer -> {
                if (answer == null || answer.isBlank()) {
                  return Flux.empty();
                }
                return Flux.just(answer);
              });
    }
    return basePrompt(agent, task).stream()
        .content()
        .map(ToolCallMarkupFilter::sanitize)
        .filter(chunk -> chunk != null && !chunk.isEmpty());
  }

  @Override
  public String invoke(AgentDefinition agent, String task) {
    String content = basePrompt(agent, task).call().content();
    return ToolCallMarkupFilter.sanitize(content == null ? "" : content);
  }

  private boolean usesTools(AgentDefinition agent) {
    return agent.toolKeys() != null && !agent.toolKeys().isEmpty();
  }

  private ChatClient.ChatClientRequestSpec basePrompt(AgentDefinition agent, String task) {
    // BARE avoids factory-wide tool defaults; attach only this worker's tools below.
    ChatClient client =
        chatClientProvider.create(TextChatOptions.defaults(), ChatClientProfile.BARE, null);
    String systemPrompt = agentSkillsRuntime.augmentSystemPrompt(agent.systemPrompt());
    ChatClient.ChatClientRequestSpec spec = client.prompt().system(systemPrompt).user(task);

    agentSkillsRuntime.skillToolCallback().ifPresent(spec::toolCallbacks);

    Object[] tools = resolveTools(agent.toolKeys());
    if (tools.length > 0) {
      return spec.tools(tools);
    }
    return spec;
  }

  private Object[] resolveTools(List<String> toolKeys) {
    if (toolKeys == null || toolKeys.isEmpty()) {
      return new Object[0];
    }
    List<Object> tools = new ArrayList<>();
    for (String key : toolKeys) {
      switch (key) {
        case "web" -> tools.add(webSearchTool);
        case "weather" -> tools.add(weatherTool);
        case "datetime" -> tools.add(dateTimeTool);
        case "document" -> tools.add(documentSearchTool);
        default -> {
          // ignore unknown keys (already validated for library saves)
        }
      }
    }
    return tools.toArray();
  }
}
