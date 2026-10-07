package com.ai.pipeline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.model.PipelineTemplateId;
import com.ai.pipeline.test.fixture.FakePipelineTemplateRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PipelineTemplateService")
class PipelineTemplateServiceTest {

  private static final String CLIENT_ID = "c:client-1";

  private FakePipelineTemplateRepository repository;
  private PipelineTemplateService useCase;

  @BeforeEach
  void setUp() {
    repository = new FakePipelineTemplateRepository();
    useCase = new PipelineTemplateService(repository);
  }

  @Test
  @DisplayName("should create template when name available")
  void shouldCreateTemplateWhenNameAvailable() {
    PipelineTemplate created =
        useCase.create(
            CLIENT_ID, "My flow", "desc", List.of("research", "analyst"), "topic", "brief", null);

    assertThat(created.getName()).isEqualTo("My flow");
    assertThat(created.getAgentTypes()).containsExactly("research", "analyst");
    assertThat(repository.saveCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("should throw when name conflict on create")
  void shouldThrowWhenNameConflictOnCreate() {
    repository.seed(
        PipelineTemplate.create(CLIENT_ID, "My flow", "", List.of("analyst"), "", "brief", null));

    assertThatThrownBy(
            () -> useCase.create(CLIENT_ID, "My flow", "", List.of("analyst"), "", "other", null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "PIPELINE_TEMPLATE_NAME_CONFLICT");
  }

  @Test
  @DisplayName("should throw when get missing")
  void shouldThrowWhenGetMissing() {
    assertThatThrownBy(() -> useCase.get(CLIENT_ID, PipelineTemplateId.generate().value()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "PIPELINE_TEMPLATE_NOT_FOUND");
  }

  @Test
  @DisplayName("should create from template when template exists")
  void shouldCreateFromTemplateWhenTemplateExists() {
    PipelineTemplate created = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

    assertThat(created.getName()).isEqualTo("Competitive intelligence");
    assertThat(created.getAgentTypes()).containsExactly("research", "analyst");
    assertThat(created.getSourceTemplateId()).isEqualTo("competitiveIntel");
    assertThat(created.getBriefPrompt()).contains("Competitive Intelligence Brief");
  }

  @Test
  @DisplayName("should create localized template when language is zh")
  void shouldCreateLocalizedTemplateWhenLanguageIsZh() {
    PipelineTemplate created = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "zh");

    assertThat(created.getName()).isEqualTo("竞品情报");
    assertThat(created.getBriefPrompt()).contains("竞品情报简报");
  }

  @Test
  @DisplayName("should suffix name when create from template conflicts")
  void shouldSuffixNameWhenCreateFromTemplateConflicts() {
    useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

    PipelineTemplate duplicate = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

    assertThat(duplicate.getName()).isEqualTo("Competitive intelligence (2)");
  }

  @Test
  @DisplayName("should disable template when set enabled false")
  void shouldDisableTemplateWhenSetEnabledFalse() {
    PipelineTemplate seeded =
        repository.seed(
            PipelineTemplate.create(
                CLIENT_ID, "My flow", "", List.of("analyst"), "", "brief", null));

    PipelineTemplate updated = useCase.setEnabled(CLIENT_ID, seeded.getId().value(), false);

    assertThat(updated.isEnabled()).isFalse();
  }
}
