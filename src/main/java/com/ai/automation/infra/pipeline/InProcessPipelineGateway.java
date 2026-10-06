package com.ai.automation.infra.pipeline;

import com.ai.automation.domain.repository.PipelineGateway;
import com.ai.pipeline.domain.exception.PipelineTemplateNotFoundException;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.domain.vo.PipelineTemplateId;
import com.ai.pipeline.service.PipelineService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Runs an owner's saved pipeline template in process as a linear agent pipeline and returns its
 * output.
 */
@Component
@RequiredArgsConstructor
public class InProcessPipelineGateway implements PipelineGateway {

  /** Same default as pipelines canvas when no real brief is configured. */
  static final String GENERIC_PLACEHOLDER =
      "Follow the configured agent pipeline for the user task.";

  private final PipelineTemplateRepository pipelineTemplateRepository;
  private final PipelineService pipelineService;

  @Override
  public String runSavedTemplate(
      String ownerKey, String pipelineTemplateId, String brief, String language) {
    PipelineTemplate template =
        pipelineTemplateRepository
            .findByIdAndOwnerKey(PipelineTemplateId.of(pipelineTemplateId), ownerKey)
            .filter(PipelineTemplate::isEnabled)
            .orElseThrow(() -> new PipelineTemplateNotFoundException(pipelineTemplateId));
    AgentPipeline pipeline = toLinearPipeline(template.getAgentTypes());
    String message =
        resolveInvokeMessage(brief, template.getShortTopic(), template.getBriefPrompt());
    return pipelineService.invokePipelineSync(message, pipeline, ownerKey, language);
  }

  /**
   * Aligns with pipelines UI: {@code topic + "\\n\\n" + briefPrompt}. Placeholder / blank schedule
   * briefs fall back to the template short topic.
   */
  static String resolveInvokeMessage(String scheduleBrief, String shortTopic, String briefPrompt) {
    String instructions = briefPrompt == null ? "" : briefPrompt.trim();
    String topic;
    if (isGenericPlaceholder(scheduleBrief)) {
      topic = shortTopic == null ? "" : shortTopic.trim();
    } else {
      topic = scheduleBrief.trim();
    }
    if (instructions.isBlank()) {
      return topic.isBlank() ? GENERIC_PLACEHOLDER : topic;
    }
    if (!topic.isBlank() && topic.contains(instructions)) {
      return topic;
    }
    if (topic.isBlank()) {
      return instructions;
    }
    return topic + "\n\n" + instructions;
  }

  /** Tells whether the brief is empty or the generic placeholder. */
  static boolean isGenericPlaceholder(String brief) {
    if (brief == null || brief.isBlank()) {
      return true;
    }
    String normalized = brief.trim().toLowerCase(Locale.ROOT);
    return normalized.equals(GENERIC_PLACEHOLDER.toLowerCase(Locale.ROOT));
  }

  /** Builds a pipeline that runs the agents one after another. */
  static AgentPipeline toLinearPipeline(List<String> agentTypes) {
    List<AgentPipeline.PipelineNode> nodes = new ArrayList<>();
    List<AgentPipeline.PipelineEdge> edges = new ArrayList<>();
    for (int i = 0; i < agentTypes.size(); i++) {
      String id = "n" + i;
      nodes.add(AgentPipeline.PipelineNode.of(id, AgentType.of(agentTypes.get(i))));
      if (i > 0) {
        edges.add(new AgentPipeline.PipelineEdge("n" + (i - 1), id));
      }
    }
    return AgentPipeline.create(nodes, edges);
  }
}
