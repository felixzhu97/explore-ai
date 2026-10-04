package com.ai.pipeline.service;

import com.ai.pipeline.domain.exception.WorkflowTemplateNameConflictException;
import com.ai.pipeline.domain.exception.WorkflowTemplateNotFoundException;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import java.util.List;
import org.springframework.stereotype.Service;

/** Manages a client's saved workflow templates, including copies of built-in catalog templates. */
@Service
public class PipelineTemplateService {

  private final WorkflowTemplateRepository repository;

  public PipelineTemplateService(WorkflowTemplateRepository repository) {
    this.repository = repository;
  }

  public List<SavedWorkflowTemplate> listLibrary(String ownerKey) {
    return repository.findAllByOwnerKey(ownerKey);
  }

  public SavedWorkflowTemplate get(String ownerKey, String id) {
    return findOwned(ownerKey, id);
  }

  /** Saves a new Pipeline Template for the owner. */
  public SavedWorkflowTemplate create(
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId) {
    assertNameAvailable(ownerKey, name, null);
    SavedWorkflowTemplate template =
        SavedWorkflowTemplate.create(
            ownerKey, name, description, agentTypes, shortTopic, briefPrompt, sourceTemplateId);
    return repository.save(template);
  }

  /** Replaces the owner's Pipeline Template content. */
  public SavedWorkflowTemplate update(
      String ownerKey,
      String id,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt) {
    SavedWorkflowTemplate template = findOwned(ownerKey, id);
    assertNameAvailable(ownerKey, name, template.getId());
    template.update(name, description, agentTypes, shortTopic, briefPrompt);
    return repository.save(template);
  }

  /** Enables or disables the owner's Pipeline Template. */
  public SavedWorkflowTemplate setEnabled(String ownerKey, String id, boolean enabled) {
    SavedWorkflowTemplate template = findOwned(ownerKey, id);
    if (enabled) {
      template.enable();
    } else {
      template.disable();
    }
    return repository.save(template);
  }

  public void delete(String ownerKey, String id) {
    findOwned(ownerKey, id);
    repository.deleteByIdAndOwnerKey(WorkflowTemplateId.of(id), ownerKey);
  }

  public List<WorkflowTemplate> listTemplates(String language) {
    return WorkflowTemplateCatalog.listAll(language);
  }

  /** Copies a built-in template into the owner's library. */
  public SavedWorkflowTemplate createFromTemplate(
      String ownerKey, String templateId, String language) {
    WorkflowTemplate template =
        WorkflowTemplateCatalog.findById(templateId, language)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown workflow template: " + templateId));
    return create(
        ownerKey,
        nextAvailableName(ownerKey, template.name()),
        template.description(),
        template.agentTypes(),
        template.shortTopic(),
        template.briefPrompt(),
        template.id());
  }

  private SavedWorkflowTemplate findOwned(String ownerKey, String id) {
    return repository
        .findByIdAndOwnerKey(WorkflowTemplateId.of(id), ownerKey)
        .orElseThrow(() -> new WorkflowTemplateNotFoundException(id));
  }

  private void assertNameAvailable(String ownerKey, String name, WorkflowTemplateId excludeId) {
    if (repository.existsByOwnerKeyAndNameIgnoringId(ownerKey, name, excludeId)) {
      throw new WorkflowTemplateNameConflictException(name);
    }
  }

  private String nextAvailableName(String ownerKey, String baseName) {
    if (!repository.existsByOwnerKeyAndNameIgnoringId(ownerKey, baseName, null)) {
      return baseName;
    }
    for (int suffix = 2; suffix <= 99; suffix++) {
      String candidate = baseName + " (" + suffix + ")";
      if (!repository.existsByOwnerKeyAndNameIgnoringId(ownerKey, candidate, null)) {
        return candidate;
      }
    }
    return baseName + " (" + WorkflowTemplateId.generate().value().substring(0, 8) + ")";
  }
}
