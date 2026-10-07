package com.ai.pipeline.infra.registry;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.common.infra.prompt.ClasspathPromptLoader;
import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.pipeline.domain.model.CustomAgent;
import com.ai.pipeline.domain.repository.AgentRegistry;
import com.ai.pipeline.domain.repository.CustomAgentRepository;
import com.ai.pipeline.service.AgentTemplate;
import com.ai.pipeline.service.AgentTemplateCatalog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builtin agent definitions from multilingual classpath templates, merged with client-owned library
 * definitions (same typeKey overrides builtin).
 */
@Component
@RequiredArgsConstructor
public class CatalogAgentRegistry implements AgentRegistry {

  private final PromptTemplates promptTemplates;
  private final CustomAgentRepository customAgentRepository;

  /** Test helper: fixed in-memory catalog (not a Spring bean). */
  public static AgentRegistry fixed(List<AgentDefinition> definitions) {
    Map<String, AgentDefinition> map = new LinkedHashMap<>();
    for (AgentDefinition definition : definitions) {
      map.put(definition.getType().value(), definition);
    }
    Map<String, AgentDefinition> fixed = Map.copyOf(map);
    return new AgentRegistry() {
      @Override
      public List<AgentDefinition> listBuiltins(String language) {
        return List.copyOf(fixed.values());
      }

      @Override
      public List<AgentDefinition> listAll(String ownerKey, String language) {
        return listBuiltins(language);
      }

      @Override
      public List<AgentDefinition> listWorkers(String ownerKey, String language) {
        List<AgentDefinition> workers = new ArrayList<>();
        for (AgentDefinition agent : listAll(ownerKey, language)) {
          if (agent.isWorker()) {
            workers.add(agent);
          }
        }
        return List.copyOf(workers);
      }

      @Override
      public Optional<AgentDefinition> findByType(
          AgentType type, String ownerKey, String language) {
        return Optional.ofNullable(fixed.get(type.value()));
      }

      @Override
      public AgentDefinition require(AgentType type, String ownerKey, String language) {
        return findByType(type, ownerKey, language)
            .orElseThrow(
                () ->
                    DomainException.notFound(
                        "AGENT_NOT_FOUND", "Unknown agent type: " + type.value()));
      }
    };
  }

  @Override
  public List<AgentDefinition> listBuiltins(String language) {
    return AgentTemplateCatalog.listAll(language).stream().map(this::toDefinition).toList();
  }

  @Override
  public List<AgentDefinition> listAll(String ownerKey, String language) {
    Map<String, AgentDefinition> byType = new LinkedHashMap<>();
    for (AgentDefinition builtin : listBuiltins(language)) {
      byType.put(builtin.getType().value(), builtin);
    }
    for (CustomAgent saved : library(ownerKey)) {
      byType.put(saved.getTypeKey(), saved.toAgentDefinition());
    }
    return List.copyOf(byType.values());
  }

  @Override
  public List<AgentDefinition> listWorkers(String ownerKey, String language) {
    List<AgentDefinition> workers = new ArrayList<>();
    for (AgentDefinition agent : listAll(ownerKey, language)) {
      if (agent.isWorker()) {
        workers.add(agent);
      }
    }
    return List.copyOf(workers);
  }

  @Override
  public Optional<AgentDefinition> findByType(AgentType type, String ownerKey, String language) {
    return library(ownerKey).stream()
        .filter(saved -> saved.hasType(type))
        .findFirst()
        .map(CustomAgent::toAgentDefinition)
        .or(
            () ->
                AgentTemplateCatalog.findByTypeKey(type.value(), language).map(this::toDefinition));
  }

  @Override
  public AgentDefinition require(AgentType type, String ownerKey, String language) {
    return findByType(type, ownerKey, language)
        .orElseThrow(
            () ->
                DomainException.notFound("AGENT_NOT_FOUND", "Unknown agent type: " + type.value()));
  }

  private List<CustomAgent> library(String ownerKey) {
    if (ownerKey == null || ownerKey.isBlank()) {
      return List.of();
    }
    return customAgentRepository.findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(
        OwnerKey.parse(ownerKey));
  }

  private AgentDefinition toDefinition(AgentTemplate template) {
    String prompt =
        ClasspathPromptLoader.joinSections(
            template.systemPrompt(), promptTemplates.getSharedStyleInstructions());
    return AgentDefinition.create(
        AgentType.of(template.typeKey()),
        template.name(),
        template.description(),
        prompt,
        template.toolKeys() == null ? List.of() : template.toolKeys(),
        AgentDefinition.RUNTIME_SINGLE);
  }
}
