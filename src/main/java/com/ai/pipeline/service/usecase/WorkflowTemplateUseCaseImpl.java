package com.ai.pipeline.service.usecase;

import com.ai.pipeline.domain.exception.WorkflowTemplateNameConflictException;
import com.ai.pipeline.domain.exception.WorkflowTemplateNotFoundException;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import com.ai.pipeline.service.WorkflowTemplate;
import com.ai.pipeline.service.WorkflowTemplateCatalog;
import java.util.List;
import org.springframework.stereotype.Service;

/** Documentation. */
@Service
public class WorkflowTemplateUseCaseImpl implements WorkflowTemplateUseCase {

  private final WorkflowTemplateRepository repository;

  /** Documentation. */
  public WorkflowTemplateUseCaseImpl(WorkflowTemplateRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SavedWorkflowTemplate> listLibrary(String clientId) {
    return repository.findAllByClientId(clientId);
  }

  @Override
  public SavedWorkflowTemplate get(String clientId, String id) {
    return findOwned(clientId, id);
  }

  @Override
  public SavedWorkflowTemplate create(
      String clientId,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId) {
    assertNameAvailable(clientId, name, null);
    SavedWorkflowTemplate template =
        SavedWorkflowTemplate.create(
            clientId, name, description, agentTypes, shortTopic, briefPrompt, sourceTemplateId);
    return repository.save(template);
  }

  @Override
  public SavedWorkflowTemplate update(
      String clientId,
      String id,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt) {
    SavedWorkflowTemplate template = findOwned(clientId, id);
    assertNameAvailable(clientId, name, template.getId());
    template.update(name, description, agentTypes, shortTopic, briefPrompt);
    return repository.save(template);
  }

  @Override
  public SavedWorkflowTemplate setEnabled(String clientId, String id, boolean enabled) {
    SavedWorkflowTemplate template = findOwned(clientId, id);
    if (enabled) {
      template.enable();
    } else {
      template.disable();
    }
    return repository.save(template);
  }

  @Override
  public void delete(String clientId, String id) {
    findOwned(clientId, id);
    repository.deleteByIdAndClientId(WorkflowTemplateId.of(id), clientId);
  }

  @Override
  public List<WorkflowTemplate> listTemplates(String language) {
    return WorkflowTemplateCatalog.listAll(language);
  }

  @Override
  public SavedWorkflowTemplate createFromTemplate(
      String clientId, String templateId, String language) {
    WorkflowTemplate template =
        WorkflowTemplateCatalog.findById(templateId, language)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown workflow template: " + templateId));
    return create(
        clientId,
        nextAvailableName(clientId, template.name()),
        template.description(),
        template.agentTypes(),
        template.shortTopic(),
        template.briefPrompt(),
        template.id());
  }

  private SavedWorkflowTemplate findOwned(String clientId, String id) {
    return repository
        .findByIdAndClientId(WorkflowTemplateId.of(id), clientId)
        .orElseThrow(() -> new WorkflowTemplateNotFoundException(id));
  }

  private void assertNameAvailable(String clientId, String name, WorkflowTemplateId excludeId) {
    if (repository.existsByClientIdAndNameIgnoringId(clientId, name, excludeId)) {
      throw new WorkflowTemplateNameConflictException(name);
    }
  }

  private String nextAvailableName(String clientId, String baseName) {
    if (!repository.existsByClientIdAndNameIgnoringId(clientId, baseName, null)) {
      return baseName;
    }
    for (int suffix = 2; suffix <= 99; suffix++) {
      String candidate = baseName + " (" + suffix + ")";
      if (!repository.existsByClientIdAndNameIgnoringId(clientId, candidate, null)) {
        return candidate;
      }
    }
    return baseName + " (" + WorkflowTemplateId.generate().value().substring(0, 8) + ")";
  }
}
