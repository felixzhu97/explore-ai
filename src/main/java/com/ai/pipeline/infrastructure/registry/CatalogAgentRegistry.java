package com.ai.pipeline.infrastructure.registry;

import com.ai.common.infrastructure.prompt.ClasspathPromptLoader;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import com.ai.pipeline.application.AgentTemplate;
import com.ai.pipeline.application.AgentTemplateCatalog;
import com.ai.pipeline.domain.exception.AgentNotFoundException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.repository.AgentRegistry;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import com.ai.pipeline.domain.vo.AgentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Builtin agent definitions from multilingual classpath templates,
 * merged with client-owned library definitions (same typeKey overrides builtin).
 */
@Component
public class CatalogAgentRegistry implements AgentRegistry {

    private final PromptTemplates promptTemplates;
    private final SavedAgentRepository savedAgentRepository;

    public CatalogAgentRegistry(
            PromptTemplates promptTemplates,
            SavedAgentRepository savedAgentRepository) {
        this.promptTemplates = promptTemplates;
        this.savedAgentRepository = savedAgentRepository;
    }

    /** Test helper: fixed in-memory catalog (not a Spring bean). */
    public static AgentRegistry fixed(List<AgentDefinition> definitions) {
        Map<String, AgentDefinition> map = new LinkedHashMap<>();
        for (AgentDefinition definition : definitions) {
            map.put(definition.type().value(), definition);
        }
        Map<String, AgentDefinition> fixed = Map.copyOf(map);
        return new AgentRegistry() {
            @Override
            public List<AgentDefinition> listBuiltins(String language) {
                return List.copyOf(fixed.values());
            }

            @Override
            public List<AgentDefinition> listAll(String clientId, String language) {
                return listBuiltins(language);
            }

            @Override
            public List<AgentDefinition> listWorkers(String clientId, String language) {
                List<AgentDefinition> workers = new ArrayList<>();
                for (AgentDefinition agent : listAll(clientId, language)) {
                    if (agent.isWorker()) {
                        workers.add(agent);
                    }
                }
                return List.copyOf(workers);
            }

            @Override
            public Optional<AgentDefinition> findByType(AgentType type, String clientId, String language) {
                return Optional.ofNullable(fixed.get(type.value()));
            }

            @Override
            public AgentDefinition require(AgentType type, String clientId, String language) {
                return findByType(type, clientId, language).orElseThrow(() -> new AgentNotFoundException(type));
            }
        };
    }

    @Override
    public List<AgentDefinition> listBuiltins(String language) {
        return AgentTemplateCatalog.listAll(language).stream()
                .map(this::toDefinition)
                .toList();
    }

    @Override
    public List<AgentDefinition> listAll(String clientId, String language) {
        Map<String, AgentDefinition> byType = new LinkedHashMap<>();
        for (AgentDefinition builtin : listBuiltins(language)) {
            byType.put(builtin.type().value(), builtin);
        }
        if (clientId != null && !clientId.isBlank()) {
            for (SavedAgentDefinition saved : savedAgentRepository.findEnabledByClientId(clientId)) {
                byType.put(saved.getTypeKey(), saved.toAgentDefinition());
            }
        }
        return List.copyOf(byType.values());
    }

    @Override
    public List<AgentDefinition> listWorkers(String clientId, String language) {
        List<AgentDefinition> workers = new ArrayList<>();
        for (AgentDefinition agent : listAll(clientId, language)) {
            if (agent.isWorker()) {
                workers.add(agent);
            }
        }
        return List.copyOf(workers);
    }

    @Override
    public Optional<AgentDefinition> findByType(AgentType type, String clientId, String language) {
        String key = type.value().toLowerCase(Locale.ROOT);
        if (clientId != null && !clientId.isBlank()) {
            for (SavedAgentDefinition saved : savedAgentRepository.findEnabledByClientId(clientId)) {
                if (saved.getTypeKey().equals(key)) {
                    return Optional.of(saved.toAgentDefinition());
                }
            }
        }
        return AgentTemplateCatalog.findByTypeKey(key, language).map(this::toDefinition);
    }

    @Override
    public AgentDefinition require(AgentType type, String clientId, String language) {
        return findByType(type, clientId, language).orElseThrow(() -> new AgentNotFoundException(type));
    }

    private AgentDefinition toDefinition(AgentTemplate template) {
        String prompt = ClasspathPromptLoader.joinSections(
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
