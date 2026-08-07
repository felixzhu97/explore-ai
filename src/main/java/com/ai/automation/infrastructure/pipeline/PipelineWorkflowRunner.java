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

@Component
public class PipelineWorkflowRunner implements WorkflowRunner {

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
        return pipelineFacade.invokePipelineSync(brief, pipeline, clientId, language);
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
