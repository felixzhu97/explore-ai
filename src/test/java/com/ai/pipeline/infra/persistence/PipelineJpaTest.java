package com.ai.pipeline.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.testsupport.AbstractDataJpaTest;
import com.ai.testsupport.JpaTestPackages;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.ai.pipeline.domain", JpaTestPackages.COMMON})
@EnableJpaRepositories(
    basePackageClasses = {
      SpringDataSavedAgentRepository.class,
      SpringDataWorkflowTemplateRepository.class
    })
@Import({
  JpaSavedAgentRepository.class,
  JpaWorkflowTemplateRepository.class,
  OwnerPartitionScope.class
})
class PipelineJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_KEY = "c:66666666-6666-6666-6666-666666666666";

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataSavedAgentRepository agentRepository;
  @Autowired private SpringDataWorkflowTemplateRepository workflowRepository;
  @Autowired private JpaSavedAgentRepository jpaAgentRepository;
  @Autowired private JpaWorkflowTemplateRepository jpaWorkflowRepository;

  @Test
  @DisplayName("should persist and reload saved agent definition when round tripping")
  void shouldPersistAndReloadSavedAgentDefinitionWhenRoundTripping() {
    SavedAgentDefinition agent =
        SavedAgentDefinition.create(
            OWNER_KEY,
            "researcher",
            "Research Agent",
            "Finds sources",
            "You research topics thoroughly",
            List.of("web_search"));

    agentRepository.saveAndFlush(agent);
    em.clear();

    SavedAgentDefinition reloaded = agentRepository.findById(agent.getId()).orElseThrow();

    assertThat(reloaded.getTypeKey()).isEqualTo("researcher");
    assertThat(reloaded.getName()).isEqualTo("Research Agent");
    assertThat(reloaded.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("should persist tool keys as json when saving agent")
  void shouldPersistToolKeysAsJsonWhenSavingAgent() {
    SavedAgentDefinition agent =
        SavedAgentDefinition.create(
            OWNER_KEY,
            "writer",
            "Writer",
            "Writes content",
            "You write clearly",
            List.of("draft", "edit"));

    agentRepository.saveAndFlush(agent);
    em.clear();

    SavedAgentDefinition reloaded = agentRepository.findById(agent.getId()).orElseThrow();

    assertThat(reloaded.getToolKeys()).containsExactly("draft", "edit");
  }

  @Test
  @DisplayName("should persist and reload workflow template when round tripping")
  void shouldPersistAndReloadWorkflowTemplateWhenRoundTripping() {
    SavedWorkflowTemplate workflow =
        SavedWorkflowTemplate.create(
            OWNER_KEY,
            "Research flow",
            "Two-step workflow",
            List.of("researcher", "writer"),
            "AI trends",
            "Summarize recent AI news",
            null);

    workflowRepository.saveAndFlush(workflow);
    em.clear();

    SavedWorkflowTemplate reloaded = workflowRepository.findById(workflow.getId()).orElseThrow();

    assertThat(reloaded.getName()).isEqualTo("Research flow");
    assertThat(reloaded.getAgentTypes()).containsExactly("researcher", "writer");
    assertThat(reloaded.getBriefPrompt()).isEqualTo("Summarize recent AI news");
  }

  @Test
  @DisplayName("should list agents by owner ordered by name ascending")
  void shouldListAgentsByOwnerOrderedByNameAscending() {
    SavedAgentDefinition beta =
        SavedAgentDefinition.create(OWNER_KEY, "beta", "Beta Agent", "desc", "prompt", List.of());
    SavedAgentDefinition alpha =
        SavedAgentDefinition.create(OWNER_KEY, "alpha", "Alpha Agent", "desc", "prompt", List.of());
    agentRepository.saveAndFlush(beta);
    agentRepository.saveAndFlush(alpha);
    em.clear();

    List<SavedAgentDefinition> agents = jpaAgentRepository.findAllByClientId(OWNER_KEY);

    assertThat(agents)
        .extracting(SavedAgentDefinition::getName)
        .containsExactly("Alpha Agent", "Beta Agent");
  }

  @Test
  @DisplayName("should find workflow by id and owner key when scoped lookup")
  void shouldFindWorkflowByIdAndOwnerKeyWhenScopedLookup() {
    SavedWorkflowTemplate workflow =
        SavedWorkflowTemplate.create(
            OWNER_KEY, "Scoped flow", "desc", List.of("researcher"), "topic", "brief", null);
    workflowRepository.saveAndFlush(workflow);
    em.clear();

    Optional<SavedWorkflowTemplate> found =
        jpaWorkflowRepository.findByIdAndClientId(workflow.getId(), OWNER_KEY);

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Scoped flow");
  }
}
