package com.ai.automation.infrastructure.pipeline;

import com.ai.automation.domain.repository.WorkflowRunner;
import com.ai.pipeline.application.usecase.PipelineFacade;
import com.ai.pipeline.domain.exception.WorkflowTemplateNotFoundException;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class PipelineWorkflowRunner implements WorkflowRunner {

    /** Same default as pipelines canvas when no real brief is configured. */
    static final String GENERIC_PLACEHOLDER =
            "Follow the configured agent pipeline for the user task.";

    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final PipelineFacade pipelineFacade;

    public PipelineWorkflowRunner(
            WorkflowTemplateRepository workflowTemplateRepository,
            PipelineFacade pipelineFacade) {
        this.workflowTemplateRepository = workflowTemplateRepository;
        this.pipelineFacade = pipelineFacade;
    }

    @Override
    public String runSavedWorkflow(
            String clientId, String workflowTemplateId, String brief, String language) {
        SavedWorkflowTemplate template = workflowTemplateRepository
                .findByIdAndClientId(WorkflowTemplateId.of(workflowTemplateId), clientId)
                .filter(SavedWorkflowTemplate::isEnabled)
                .orElseThrow(() -> new WorkflowTemplateNotFoundException(workflowTemplateId));
        AgentPipeline pipeline = toLinearPipeline(template.getAgentTypes());
        String message = resolveInvokeMessage(
                brief, template.getShortTopic(), template.getBriefPrompt());
        return pipelineFacade.invokePipelineSync(message, pipeline, clientId, language);
    }

    /**
     * Aligns with pipelines UI: {@code topic + "\\n\\n" + briefPrompt}.
     * Placeholder / blank schedule briefs fall back to the template short topic.
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

    static boolean isGenericPlaceholder(String brief) {
        if (brief == null || brief.isBlank()) {
            return true;
        }
        String normalized = brief.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(GENERIC_PLACEHOLDER.toLowerCase(Locale.ROOT));
    }

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
