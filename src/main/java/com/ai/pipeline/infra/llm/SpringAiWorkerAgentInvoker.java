package com.ai.pipeline.infra.llm;

import com.ai.common.infra.llm.ToolCallMarkupFilter;
import com.ai.common.infra.skills.AgentSkillsRuntime;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.DocumentSearchTool;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.common.service.llm.WebSearchTool;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.service.WorkerAgentInvoker;
import com.ai.tools.infra.tools.DateTimeTools;
import com.ai.tools.infra.tools.WeatherTools;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Spring AI worker invoker that prompts an agent with its system prompt and allowed tools. */
@Component
@RequiredArgsConstructor
public class SpringAiWorkerAgentInvoker implements WorkerAgentInvoker {

  private final ChatClientProvider chatClientProvider;
  private final DocumentSearchTool documentSearchTool;
  private final WebSearchTool webSearchTool;
  private final WeatherTools weatherTool;
  private final DateTimeTools dateTimeTool;
  private final AgentSkillsRuntime agentSkillsRuntime;

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
    return buildBasePrompt(agent, task).stream()
        .content()
        .map(ToolCallMarkupFilter::sanitize)
        .filter(chunk -> chunk != null && !chunk.isEmpty());
  }

  @Override
  public String invoke(AgentDefinition agent, String task) {
    String content = buildBasePrompt(agent, task).call().content();
    return ToolCallMarkupFilter.sanitize(content == null ? "" : content);
  }

  private boolean usesTools(AgentDefinition agent) {
    return agent.getToolKeys() != null && !agent.getToolKeys().isEmpty();
  }

  private ChatClient.ChatClientRequestSpec buildBasePrompt(AgentDefinition agent, String task) {
    // BARE avoids factory-wide tool defaults; attach only this worker's tools below.
    ChatClient client =
        chatClientProvider.create(TextChatOptions.defaults(), ChatClientProfile.BARE, null);
    String systemPrompt = agentSkillsRuntime.augmentSystemPrompt(agent.getSystemPrompt());
    ChatClient.ChatClientRequestSpec spec = client.prompt().system(systemPrompt).user(task);

    agentSkillsRuntime.findSkillToolCallback().ifPresent(spec::toolCallbacks);

    Object[] tools = resolveTools(agent.getToolKeys());
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
