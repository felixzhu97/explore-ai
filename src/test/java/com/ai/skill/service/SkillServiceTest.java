package com.ai.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.model.SkillId;
import com.ai.skill.domain.repository.SkillRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService")
class SkillServiceTest {

  private static final String CLIENT_ID = "c:client-1";
  private static final OwnerKey OWNER = OwnerKey.parseKey(CLIENT_ID);

  @Mock private SkillRepository repository;
  @InjectMocks private SkillService useCase;

  @Test
  @DisplayName("should create skill when name available")
  void shouldCreateSkillWhenNameAvailable() {
    when(repository.save(any(Skill.class))).then(returnsFirstArg());

    Skill created =
        useCase.createSkill(
            CLIENT_ID, "Brief Style", "Short answers", "Be concise.", List.of("Read"));

    assertThat(created.getName()).isEqualTo("Brief Style");
    verify(repository).save(created);
  }

  @Test
  @DisplayName("should throw when name conflict on create")
  void shouldThrowWhenNameConflictOnCreate() {
    when(repository.existsByOwnerKeyAndNameIgnoringId(OWNER, "Brief Style", null)).thenReturn(true);

    assertThatThrownBy(() -> useCase.createSkill(CLIENT_ID, "Brief Style", "", "Other", List.of()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "SKILL_NAME_CONFLICT");
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("should build the active skills prompt from the valid skill ids only")
  void shouldBuildTheActiveSkillsPromptFromTheValidSkillIdsOnly() {
    Skill brief = Skill.createSkill(CLIENT_ID, "Brief Style", "", "Be short.", List.of());
    when(repository.findEnabledByOwnerKeyAndIds(OWNER, List.of(brief.getId())))
        .thenReturn(List.of(brief));

    String prompt =
        useCase
            .buildSkillsPrompt(CLIENT_ID, List.of(brief.getId().toString(), "not-a-uuid", " "))
            .orElseThrow();

    assertThat(prompt).contains("## Active Skills").contains("### Brief Style");
  }

  @Test
  @DisplayName("should return no prompt when none of the skill ids resolve")
  void shouldReturnNoPromptWhenNoneOfTheSkillIdsResolve() {
    assertThat(useCase.buildSkillsPrompt(CLIENT_ID, List.of("not-a-uuid"))).isEmpty();
    assertThat(useCase.buildSkillsPrompt(CLIENT_ID, null)).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("should throw when get missing")
  void shouldThrowWhenGetMissing() {
    assertThatThrownBy(() -> useCase.getSkill(CLIENT_ID, SkillId.generateId().toString()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "SKILL_NOT_FOUND");
  }

  @Test
  @DisplayName("should create localized skill from template")
  void shouldCreateLocalizedSkillFromTemplate() {
    when(repository.save(any(Skill.class))).then(returnsFirstArg());

    Skill english = useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");
    Skill chinese = useCase.createFromTemplate(CLIENT_ID, "brief-style", "zh");

    assertThat(english.getName()).isEqualTo("Brief Style");
    assertThat(english.getInstructions()).contains("Lead with the direct answer");
    assertThat(english.getAllowedTools()).isEmpty();
    assertThat(chinese.getName()).isEqualTo("简洁风格");
    assertThat(chinese.getInstructions()).contains("先用一两句话给出直接答案");
  }

  @Test
  @DisplayName("should suffix name when create from template conflicts")
  void shouldSuffixNameWhenCreateFromTemplateConflicts() {
    when(repository.existsByOwnerKeyAndNameIgnoringId(eq(OWNER), anyString(), isNull()))
        .thenAnswer(call -> "Brief Style".equals(call.getArgument(1)));
    when(repository.save(any(Skill.class))).then(returnsFirstArg());

    Skill duplicate = useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");

    assertThat(duplicate.getName()).isEqualTo("Brief Style (2)");
  }

  @Test
  @DisplayName("should disable skill when set enabled false")
  void shouldDisableSkillWhenSetEnabledFalse() {
    Skill skill = Skill.createSkill(CLIENT_ID, "Brief Style", "", "Instructions", List.of());
    when(repository.findByIdAndOwnerKey(skill.getId(), OWNER)).thenReturn(Optional.of(skill));
    when(repository.save(skill)).thenReturn(skill);

    Skill updated = useCase.setEnabled(CLIENT_ID, skill.getId().toString(), false);

    assertThat(updated.isEnabled()).isFalse();
  }
}
