package com.ai.pipeline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.model.PipelineTemplateId;
import com.ai.pipeline.domain.repository.PipelineTemplateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineTemplateService")
class PipelineTemplateServiceTest {

  private static final String CLIENT_ID = "c:client-1";
  private static final OwnerKey OWNER = OwnerKey.parse(CLIENT_ID);

  @Mock private PipelineTemplateRepository repository;
  @InjectMocks private PipelineTemplateService useCase;

  @Test
  @DisplayName("should create template when name available")
  void shouldCreateTemplateWhenNameAvailable() {
    when(repository.save(any(PipelineTemplate.class))).then(returnsFirstArg());

    PipelineTemplate created =
        useCase.create(
            CLIENT_ID, "My flow", "desc", List.of("research", "analyst"), "topic", "brief", null);

    assertThat(created.getName()).isEqualTo("My flow");
    assertThat(created.getAgentTypes()).containsExactly("research", "analyst");
    verify(repository).save(created);
  }

  @Test
  @DisplayName("should throw when name conflict on create")
  void shouldThrowWhenNameConflictOnCreate() {
    when(repository.existsByOwnerKeyAndNameIgnoringId(OWNER, "My flow", null)).thenReturn(true);

    assertThatThrownBy(
            () -> useCase.create(CLIENT_ID, "My flow", "", List.of("analyst"), "", "other", null))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "PIPELINE_TEMPLATE_NAME_CONFLICT");
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("should throw when get missing")
  void shouldThrowWhenGetMissing() {
    assertThatThrownBy(() -> useCase.get(CLIENT_ID, PipelineTemplateId.generate().toString()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "PIPELINE_TEMPLATE_NOT_FOUND");
  }

  @Test
  @DisplayName("should create localized template from catalog")
  void shouldCreateLocalizedTemplateFromCatalog() {
    when(repository.save(any(PipelineTemplate.class))).then(returnsFirstArg());

    PipelineTemplate english = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");
    PipelineTemplate chinese = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "zh");

    assertThat(english.getName()).isEqualTo("Competitive intelligence");
    assertThat(english.getAgentTypes()).containsExactly("research", "analyst");
    assertThat(english.getSourceTemplateId()).isEqualTo("competitiveIntel");
    assertThat(english.getBriefPrompt()).contains("Competitive Intelligence Brief");
    assertThat(chinese.getName()).isEqualTo("竞品情报");
    assertThat(chinese.getBriefPrompt()).contains("竞品情报简报");
  }

  @Test
  @DisplayName("should suffix name when create from template conflicts")
  void shouldSuffixNameWhenCreateFromTemplateConflicts() {
    when(repository.existsByOwnerKeyAndNameIgnoringId(eq(OWNER), anyString(), isNull()))
        .thenAnswer(call -> "Competitive intelligence".equals(call.getArgument(1)));
    when(repository.save(any(PipelineTemplate.class))).then(returnsFirstArg());

    PipelineTemplate duplicate = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

    assertThat(duplicate.getName()).isEqualTo("Competitive intelligence (2)");
  }

  @Test
  @DisplayName("should disable template when set enabled false")
  void shouldDisableTemplateWhenSetEnabledFalse() {
    PipelineTemplate template =
        PipelineTemplate.create(CLIENT_ID, "My flow", "", List.of("analyst"), "", "brief", null);
    when(repository.findByIdAndOwnerKey(template.getId(), OWNER)).thenReturn(Optional.of(template));
    when(repository.save(template)).thenReturn(template);

    PipelineTemplate updated = useCase.setEnabled(CLIENT_ID, template.getId().toString(), false);

    assertThat(updated.isEnabled()).isFalse();
  }
}
