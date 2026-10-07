package com.ai.pipeline.infra.registry;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.pipeline.domain.model.SavedAgentId;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogAgentRegistry")
class CatalogAgentRegistryTest {

  private InMemorySavedAgentRepository savedAgents;
  private CatalogAgentRegistry registry;

  @BeforeEach
  void setUp() {
    savedAgents = new InMemorySavedAgentRepository();
    registry = new CatalogAgentRegistry(new PromptTemplates(), savedAgents);
  }

  @Test
  void shouldListBuiltinsWhenNoClientOverrides() {
    List<AgentDefinition> builtins = registry.listBuiltins("en");
    assertThat(builtins).isNotEmpty();
    assertThat(registry.listAll("c:client-a", "en")).hasSameSizeAs(builtins);
  }

  @Test
  void shouldOverrideBuiltinWithEnabledClientDefinition() {
    String typeKey = registry.listWorkers("c:client-a", "en").getFirst().getType().value();
    savedAgents.save(
        SavedAgent.create(
            "c:client-a",
            typeKey,
            "Override Name",
            "override desc",
            "You are an override.",
            List.of("web")));

    Optional<AgentDefinition> found =
        registry.findByType(AgentType.of(typeKey), "c:client-a", "en");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Override Name");
    assertThat(found.get().getSystemPrompt()).isEqualTo("You are an override.");

    assertThat(registry.listAll("c:client-a", "en"))
        .anySatisfy(
            agent -> {
              if (agent.getType().value().equals(typeKey)) {
                assertThat(agent.getName()).isEqualTo("Override Name");
              }
            });
  }

  @Test
  void shouldIgnoreDisabledClientDefinition() {
    String typeKey = registry.listWorkers("c:client-a", "en").getFirst().getType().value();
    String builtinName = registry.require(AgentType.of(typeKey), "c:client-a", "en").getName();
    SavedAgent disabled =
        SavedAgent.create(
            "c:client-a", typeKey, "Disabled Override", "d", "Disabled prompt", List.of());
    disabled.disable();
    savedAgents.save(disabled);

    AgentDefinition effective = registry.require(AgentType.of(typeKey), "c:client-a", "en");
    assertThat(effective.getName()).isEqualTo(builtinName);
    assertThat(effective.getName()).isNotEqualTo("Disabled Override");
  }

  @Test
  void shouldIncludeCustomTypeFromEnabledLibrary() {
    savedAgents.save(
        SavedAgent.create(
            "c:client-a", "custom_writer", "Writer", "writes", "You write.", List.of("document")));

    assertThat(registry.listAll("c:client-a", "en"))
        .anySatisfy(
            agent -> {
              assertThat(agent.getType().value()).isEqualTo("custom_writer");
              assertThat(agent.getName()).isEqualTo("Writer");
            });
    assertThat(registry.findByType(AgentType.of("custom_writer"), "other-client", "en")).isEmpty();
  }

  @Test
  @DisplayName("should keep built-in order and append saved agents with new types last")
  void shouldKeepBuiltInOrderAndAppendSavedAgentsWithNewTypesLast() {
    List<String> builtinTypes =
        registry.listBuiltins("en").stream().map(agent -> agent.getType().value()).toList();
    savedAgents.save(
        SavedAgent.create("c:client-a", "legal", "Legal", "", "Check contracts.", List.of()));

    List<String> allTypes =
        registry.listAll("c:client-a", "en").stream()
            .map(agent -> agent.getType().value())
            .toList();

    assertThat(allTypes.subList(0, builtinTypes.size())).isEqualTo(builtinTypes);
    assertThat(allTypes.getLast()).isEqualTo("legal");
  }

  @Test
  @DisplayName("should find a saved agent whatever the case of the requested type")
  void shouldFindASavedAgentWhateverTheCaseOfTheRequestedType() {
    savedAgents.save(
        SavedAgent.create("c:client-a", "legal", "Legal", "", "Check contracts.", List.of()));

    assertThat(registry.findByType(AgentType.of(" Legal "), "c:client-a", "en"))
        .map(AgentDefinition::getName)
        .contains("Legal");
  }

  private static final class InMemorySavedAgentRepository implements SavedAgentRepository {
    private final List<SavedAgent> agents = new ArrayList<>();

    @Override
    public SavedAgent save(SavedAgent agent) {
      agents.removeIf(existing -> existing.getId().equals(agent.getId()));
      agents.add(agent);
      return agent;
    }

    @Override
    public Optional<SavedAgent> findByIdAndOwnerKey(SavedAgentId id, String ownerKey) {
      return agents.stream()
          .filter(a -> a.getId().equals(id) && a.getOwnerKeyValue().equals(ownerKey))
          .findFirst();
    }

    @Override
    public List<SavedAgent> findAllByOwnerKey(String ownerKey) {
      return agents.stream().filter(a -> a.getOwnerKeyValue().equals(ownerKey)).toList();
    }

    @Override
    public List<SavedAgent> findEnabledByOwnerKey(String ownerKey) {
      return agents.stream()
          .filter(a -> a.getOwnerKeyValue().equals(ownerKey) && a.isEnabled())
          .toList();
    }

    @Override
    public void deleteByIdAndOwnerKey(SavedAgentId id, String ownerKey) {
      agents.removeIf(a -> a.getId().equals(id) && a.getOwnerKeyValue().equals(ownerKey));
    }

    @Override
    public boolean existsByOwnerKeyAndTypeKeyIgnoringId(
        String ownerKey, String typeKey, SavedAgentId excludeId) {
      return agents.stream()
          .anyMatch(
              a ->
                  a.getOwnerKeyValue().equals(ownerKey)
                      && a.getTypeKey().equals(typeKey)
                      && (excludeId == null || !a.getId().equals(excludeId)));
    }
  }
}
