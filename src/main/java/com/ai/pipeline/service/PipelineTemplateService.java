package com.ai.pipeline.service;

import com.ai.common.domain.model.DomainStrings;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.model.PipelineTemplateId;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages a client's saved pipeline templates, including copies of built-in catalog templates. */
@Service
@Transactional
@RequiredArgsConstructor
public class PipelineTemplateService {

  private final PipelineTemplateRepository repository;

  /** Lists the built-in pipeline templates. */
  public List<BuiltinPipelineTemplate> listTemplates(String language) {
    return PipelineTemplateCatalog.listAll(language);
  }

  /** Lists the owner's saved pipelines. */
  public List<PipelineTemplate> listLibrary(String ownerKey) {
    return repository.findAllByOwnerKeyOrderByNameAsc(OwnerKey.parse(ownerKey));
  }

  /** Copies a built-in template into the owner's library. */
  public PipelineTemplate createFromTemplate(String ownerKey, String templateId, String language) {
    BuiltinPipelineTemplate template =
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
    template.changeEnabled(enabled);
    return repository.save(template);
  }

  /** Deletes the owner's saved pipeline. */
  public void delete(String ownerKey, String id) {
    findOwned(ownerKey, id);
    repository.deleteByIdAndOwnerKey(PipelineTemplateId.of(id), OwnerKey.parse(ownerKey));
  }

  /** Returns the owner's saved pipeline. */
  public PipelineTemplate get(String ownerKey, String id) {
    return findOwned(ownerKey, id);
  }

  private PipelineTemplate findOwned(String ownerKey, String id) {
    return repository
        .findByIdAndOwnerKey(PipelineTemplateId.of(id), OwnerKey.parse(ownerKey))
        .orElseThrow(
            () ->
                DomainException.notFound(
                    "PIPELINE_TEMPLATE_NOT_FOUND", "Pipeline template not found: " + id));
  }

  private void assertNameAvailable(String ownerKey, String name, PipelineTemplateId excludeId) {
    String normalized = DomainStrings.normalizeName(name);
    if (repository.existsByOwnerKeyAndNameIgnoringId(
        OwnerKey.parse(ownerKey), normalized, excludeId)) {
      throw DomainException.conflict(
          "PIPELINE_TEMPLATE_NAME_CONFLICT",
          "Pipeline template name already exists: " + normalized);
    }
  }

  private String findNextAvailableName(String ownerKey, String baseName) {
    OwnerKey owner = OwnerKey.parse(ownerKey);
    return DomainStrings.copyNameCandidates(baseName, DomainStrings.DEFAULT_NAME_MAX)
        .filter(name -> !repository.existsByOwnerKeyAndNameIgnoringId(owner, name, null))
        .findFirst()
        .orElseGet(
            () ->
                DomainStrings.copyName(
                    baseName,
                    PipelineTemplateId.generate().toString().substring(0, 8),
                    DomainStrings.DEFAULT_NAME_MAX));
  }
}
