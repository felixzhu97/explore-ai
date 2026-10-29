package com.ai.agent.service;

import com.ai.agent.domain.model.AgentRun;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.service.PipelineService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Sibling-facing Agent façade. Delegates to Pipeline workers; does not own LLM or canvas graph
 * logic.
 */
@Service
@RequiredArgsConstructor
public class AgentService {

  /** Catalog type used when A2A skill is omitted. */
  public static final String DEFAULT_AGENT_TYPE = "research";

  private final PipelineService pipelineService;

  /** Lists runnable Agent types for the owner. */
  public List<AgentDefinition> listAgents(String ownerKey, String language) {
    return pipelineService.listAgents(ownerKey, language);
  }

  /** Returns one Agent Definition by type. */
  public AgentDefinition getAgent(String agentType, String ownerKey, String language) {
    return pipelineService.getHealth(agentType, ownerKey, language);
  }

  /** Runs one Agent and returns a completed Agent Run. */
  public AgentRun invoke(String agentType, String message, String ownerKey, String language) {
    String reply = pipelineService.invokeAgentSync(agentType, message, ownerKey, language);
    return AgentRun.createCompleted(agentType, message, reply);
  }

  /** Streams one Agent run as SSE (same event shape as Pipeline direct invoke). */
  public Flux<ServerSentEvent<String>> invokeStream(
      String agentType, String message, String ownerKey, String language) {
    return pipelineService.invokeAgent(agentType, message, ownerKey, language);
  }

  /** Resolves Accept-Language / ?lang= the same way Pipeline does. */
  public static String resolveLanguage(String lang, String acceptLanguage) {
    if (lang != null && !lang.isBlank()) {
      return lang.trim();
    }
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return "en";
    }
    String primary = acceptLanguage.split(",")[0].trim();
    int dash = primary.indexOf('-');
    String tag = dash > 0 ? primary.substring(0, dash) : primary;
    return tag.toLowerCase(Locale.ROOT);
  }
}
