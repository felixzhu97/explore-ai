package com.ai.automation.infra.pipeline;

import com.ai.automation.domain.repository.PipelineGateway;
import com.ai.pipeline.domain.exception.PipelineTemplateNotFoundException;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import com.ai.pipeline.domain.vo.PipelineTemplateId;
import com.ai.pipeline.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Runs an owner's saved pipeline template in process as a linear agent pipeline and returns its
 * output.
 */
@Component
@RequiredArgsConstructor
public class InProcessPipelineGateway implements PipelineGateway {

  private final PipelineTemplateRepository pipelineTemplateRepository;
  private final PipelineService pipelineService;

  @Override
  public String runSavedTemplate(
      String ownerKey, String pipelineTemplateId, String brief, String language) {
    PipelineTemplate template =
        pipelineTemplateRepository
            .findByIdAndOwnerKey(PipelineTemplateId.of(pipelineTemplateId), ownerKey)
            .filter(PipelineTemplate::isRunnable)
            .orElseThrow(() -> new PipelineTemplateNotFoundException(pipelineTemplateId));
    return pipelineService.invokePipelineSync(
        template.composeInvokeMessage(brief), template.toLinearPipeline(), ownerKey, language);
  }
}
