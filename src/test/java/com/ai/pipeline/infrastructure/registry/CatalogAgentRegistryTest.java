package com.ai.pipeline.infrastructure.registry;

import com.ai.common.infrastructure.prompt.PromptTemplates;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.domain.vo.SavedAgentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAgentRegistryTest {

    private InMemorySavedAgentRepository savedAgents;
    private CatalogAgentRegistry registry;

    @BeforeEach
    void setUp() {
        savedAgents = new InMemorySavedAgentRepository();
        registry = new CatalogAgentRegistry(new PromptTemplates(), savedAgents);
    }

    @Test
    void should_list_builtins_when_no_client_overrides() {
        List<AgentDefinition> builtins = registry.listBuiltins("en");
        assertThat(builtins).isNotEmpty();
        assertThat(registry.listAll("client-a", "en")).hasSameSizeAs(builtins);
    }

    @Test
    void should_override_builtin_with_enabled_client_definition() {
        String typeKey = registry.listWorkers("client-a", "en").getFirst().type().value();
        savedAgents.save(SavedAgentDefinition.restore(
                SavedAgentId.generate(),
                "client-a",
                typeKey,
                "Override Name",
                "override desc",
                "You are an override.",
                List.of("web"),
                true,
                Instant.now(),
                Instant.now()));

        Optional<AgentDefinition> found = registry.findByType(AgentType.of(typeKey), "client-a", "en");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Override Name");
        assertThat(found.get().systemPrompt()).isEqualTo("You are an override.");

        assertThat(registry.listAll("client-a", "en"))
                .anySatisfy(agent -> {
                    if (agent.type().value().equals(typeKey)) {
                        assertThat(agent.name()).isEqualTo("Override Name");
                    }
                });
    }

    @Test
    void should_ignore_disabled_client_definition() {
        String typeKey = registry.listWorkers("client-a", "en").getFirst().type().value();
        String builtinName = registry.require(AgentType.of(typeKey), "client-a", "en").name();
        savedAgents.save(SavedAgentDefinition.restore(
                SavedAgentId.generate(),
                "client-a",
                typeKey,
                "Disabled Override",
                "d",
                "Disabled prompt",
                List.of(),
                false,
                Instant.now(),
                Instant.now()));

        AgentDefinition effective = registry.require(AgentType.of(typeKey), "client-a", "en");
        assertThat(effective.name()).isEqualTo(builtinName);
        assertThat(effective.name()).isNotEqualTo("Disabled Override");
    }

    @Test
    void should_include_custom_type_from_enabled_library() {
        savedAgents.save(SavedAgentDefinition.create(
                "client-a",
                "custom_writer",
                "Writer",
                "writes",
                "You write.",
                List.of("document")));

        assertThat(registry.listAll("client-a", "en"))
                .anySatisfy(agent -> {
                    assertThat(agent.type().value()).isEqualTo("custom_writer");
                    assertThat(agent.name()).isEqualTo("Writer");
                });
        assertThat(registry.findByType(AgentType.of("custom_writer"), "other-client", "en")).isEmpty();
    }

    private static final class InMemorySavedAgentRepository implements SavedAgentRepository {
        private final List<SavedAgentDefinition> agents = new ArrayList<>();

        @Override
        public SavedAgentDefinition save(SavedAgentDefinition agent) {
            agents.removeIf(existing -> existing.getId().equals(agent.getId()));
            agents.add(agent);
            return agent;
        }

        @Override
        public Optional<SavedAgentDefinition> findByIdAndClientId(SavedAgentId id, String clientId) {
            return agents.stream()
                    .filter(a -> a.getId().equals(id) && a.getClientId().equals(clientId))
                    .findFirst();
        }

        @Override
        public List<SavedAgentDefinition> findAllByClientId(String clientId) {
            return agents.stream().filter(a -> a.getClientId().equals(clientId)).toList();
        }

        @Override
        public List<SavedAgentDefinition> findEnabledByClientId(String clientId) {
            return agents.stream()
                    .filter(a -> a.getClientId().equals(clientId) && a.isEnabled())
                    .toList();
        }

        @Override
        public void deleteByIdAndClientId(SavedAgentId id, String clientId) {
            agents.removeIf(a -> a.getId().equals(id) && a.getClientId().equals(clientId));
        }

        @Override
        public boolean existsByClientIdAndTypeKeyIgnoringId(String clientId, String typeKey, SavedAgentId excludeId) {
            return agents.stream().anyMatch(a ->
                    a.getClientId().equals(clientId)
                            && a.getTypeKey().equals(typeKey)
                            && (excludeId == null || !a.getId().equals(excludeId)));
        }
    }
}
