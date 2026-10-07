package com.ai.pipeline.infra.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.pipeline.domain.model.CustomAgent;
import com.ai.pipeline.domain.repository.CustomAgentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogAgentRegistry")
class CatalogAgentRegistryTest {

  private static final OwnerKey OWNER = OwnerKey.parseKey("c:client-a");

  @Mock private CustomAgentRepository customAgents;
  private CatalogAgentRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new CatalogAgentRegistry(new PromptTemplates(), customAgents);
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
    library(
        CustomAgent.createAgent(
            "c:client-a",
            typeKey,
            "Override Name",
            "override desc",
            "You are an override.",
            List.of("web")));

    Optional<AgentDefinition> found =
        registry.findByType(AgentType.createType(typeKey), "c:client-a", "en");
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
  void shouldIncludeCustomTypeFromEnabledLibrary() {
    library(
        CustomAgent.createAgent(
            "c:client-a", "custom_writer", "Writer", "writes", "You write.", List.of("document")));

    assertThat(registry.listAll("c:client-a", "en"))
        .anySatisfy(
            agent -> {
              assertThat(agent.getType().value()).isEqualTo("custom_writer");
              assertThat(agent.getName()).isEqualTo("Writer");
            });
  }

  @Test
  @DisplayName("should keep built-in order and append custom agents with new types last")
  void shouldKeepBuiltInOrderAndAppendCustomAgentsWithNewTypesLast() {
    List<String> builtinTypes =
        registry.listBuiltins("en").stream().map(agent -> agent.getType().value()).toList();
    library(
        CustomAgent.createAgent("c:client-a", "legal", "Legal", "", "Check contracts.", List.of()));

    List<String> allTypes =
        registry.listAll("c:client-a", "en").stream()
            .map(agent -> agent.getType().value())
            .toList();

    assertThat(allTypes.subList(0, builtinTypes.size())).isEqualTo(builtinTypes);
    assertThat(allTypes.getLast()).isEqualTo("legal");
  }

  @Test
  @DisplayName("should find a custom agent whatever the case of the requested type")
  void shouldFindACustomAgentWhateverTheCaseOfTheRequestedType() {
    library(
        CustomAgent.createAgent("c:client-a", "legal", "Legal", "", "Check contracts.", List.of()));

    assertThat(registry.findByType(AgentType.createType(" Legal "), "c:client-a", "en"))
        .map(AgentDefinition::getName)
        .contains("Legal");
  }

  private void library(CustomAgent... agents) {
    when(customAgents.findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(OWNER))
        .thenReturn(List.of(agents));
  }
}
