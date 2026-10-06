package com.ai.pipeline.service;

import com.ai.pipeline.domain.exception.PipelineTemplateNameConflictException;
import com.ai.pipeline.domain.exception.PipelineTemplateNotFoundException;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import com.ai.pipeline.domain.vo.PipelineTemplateId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Manages a client's saved pipeline templates, including copies of built-in catalog templates. */
@Service
@RequiredArgsConstructor
public class PipelineTemplateService {

  private final PipelineTemplateRepository repository;

  public List<PipelineTemplateDefinition> listTemplates(String language) {
    return PipelineTemplateCatalog.listAll(language);
  }

  public List<PipelineTemplate> listLibrary(String ownerKey) {
    return repository.findAllByOwnerKey(ownerKey);
  }

  /** Copies a built-in template into the owner's library. */
  public PipelineTemplate createFromTemplate(String ownerKey, String templateId, String language) {
    PipelineTemplateDefinition template =
        PipelineTemplateCatalog.findById(templateId, language)
            .orElseThrow(
                () -> new IllegalArgumentException("Unknown pipeline template: " + templateId));
    return create(
        ownerKey,
        findNextAvailableName(ownerKey, template.name()),
        template.description(),
        template.agentTypes(),
        template.shortTopic(),
        template.briefPrompt(),
        template.id());
  }

  /** Saves a new Pipeline Template for the owner. */
  public PipelineTemplate create(
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId) {
    assertNameAvailable(ownerKey, name, null);
    PipelineTemplate template =
        PipelineTemplate.create(
            ownerKey, name, description, agentTypes, shortTopic, briefPrompt, sourceTemplateId);
    return repository.save(template);
  }

  /** Replaces the owner's Pipeline Template content. */
  public PipelineTemplate update(
      String ownerKey,
      String id,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt) {
    PipelineTemplate template = findOwned(ownerKey, id);
    assertNameAvailable(ownerKey, name, template.getId());
    template.update(name, description, agentTypes, shortTopic, briefPrompt);
    return repository.save(template);
  }

  /** Enables or disables the owner's Pipeline Template. */
  public PipelineTemplate setEnabled(String ownerKey, String id, boolean enabled) {
    PipelineTemplate template = findOwned(ownerKey, id);
    if (enabled) {
      template.enable();
    } else {
      template.disable();
    }
    return repository.save(template);
  }

  public void delete(String ownerKey, String id) {
    findOwned(ownerKey, id);
    repository.deleteByIdAndOwnerKey(PipelineTemplateId.of(id), ownerKey);
  }

  public PipelineTemplate get(String ownerKey, String id) {
    return findOwned(ownerKey, id);
  }

  private PipelineTemplate findOwned(String ownerKey, String id) {
    return repository
        .findByIdAndOwnerKey(PipelineTemplateId.of(id), ownerKey)
        .orElseThrow(() -> new PipelineTemplateNotFoundException(id));
  }

  private void assertNameAvailable(String ownerKey, String name, PipelineTemplateId excludeId) {
    if (repository.existsByOwnerKeyAndNameIgnoringId(ownerKey, name, excludeId)) {
      throw new PipelineTemplateNameConflictException(name);
    }
  }

  private String findNextAvailableName(String ownerKey, String baseName) {
    if (!repository.existsByOwnerKeyAndNameIgnoringId(ownerKey, baseName, null)) {
      return baseName;
    }
    for (int suffix = 2; suffix <= 99; suffix++) {
      String candidate = baseName + " (" + suffix + ")";
      if (!repository.existsByOwnerKeyAndNameIgnoringId(ownerKey, candidate, null)) {
        return candidate;
      }
    }
    return baseName + " (" + PipelineTemplateId.generate().value().substring(0, 8) + ")";
  }
}
