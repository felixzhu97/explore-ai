package com.ai.skill.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.exception.DomainException;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.model.SkillId;
import com.ai.skill.test.fixture.FakeSkillRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SkillService")
class SkillServiceTest {

  private static final String CLIENT_ID = "c:client-1";

  private FakeSkillRepository repository;
  private SkillService useCase;

  @BeforeEach
  void setUp() {
    repository = new FakeSkillRepository();
    useCase = new SkillService(repository);
  }

  @Test
  @DisplayName("should create skill when name available")
  void shouldCreateSkillWhenNameAvailable() {
    Skill created =
        useCase.create(CLIENT_ID, "Brief Style", "Short answers", "Be concise.", List.of("Read"));

    assertThat(created.getName()).isEqualTo("Brief Style");
    assertThat(repository.saveCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("should throw when name conflict on create")
  void shouldThrowWhenNameConflictOnCreate() {
    repository.seed(Skill.create(CLIENT_ID, "Brief Style", "", "Instructions", List.of()));

    assertThatThrownBy(() -> useCase.create(CLIENT_ID, "Brief Style", "", "Other", List.of()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "SKILL_NAME_CONFLICT");
  }

  @Test
  @DisplayName("should build the active skills prompt from enabled owned skills only")
  void shouldBuildTheActiveSkillsPromptFromEnabledOwnedSkillsOnly() {
    Skill brief =
        repository.seed(Skill.create(CLIENT_ID, "Brief Style", "", "Be short.", List.of()));
    Skill off = repository.seed(Skill.create(CLIENT_ID, "Formal", "", "Be formal.", List.of()));
    off.disable();
    Skill foreign =
        repository.seed(Skill.create("c:client-2", "Pirate", "", "Talk like a pirate.", List.of()));

    String prompt =
        useCase
            .activeSkillsPrompt(
                CLIENT_ID,
                List.of(
                    brief.getId().value(),
                    off.getId().value(),
                    foreign.getId().value(),
                    "not-a-uuid",
                    " "))
            .orElseThrow();

    assertThat(prompt).contains("## Active Skills").contains("### Brief Style");
    assertThat(prompt).doesNotContain("Formal").doesNotContain("Pirate");
  }

  @Test
  @DisplayName("should return no prompt when none of the skill ids resolve")
  void shouldReturnNoPromptWhenNoneOfTheSkillIdsResolve() {
    assertThat(useCase.activeSkillsPrompt(CLIENT_ID, List.of("not-a-uuid"))).isEmpty();
    assertThat(useCase.activeSkillsPrompt(CLIENT_ID, null)).isEmpty();
  }

  @Test
  @DisplayName("should return skill when get existing")
  void shouldReturnSkillWhenGetExisting() {
    Skill seeded =
        repository.seed(Skill.create(CLIENT_ID, "Brief Style", "", "Instructions", List.of()));

    Skill found = useCase.get(CLIENT_ID, seeded.getId().value());

    assertThat(found.getId()).isEqualTo(seeded.getId());
  }

  @Test
  @DisplayName("should throw when get missing")
  void shouldThrowWhenGetMissing() {
    assertThatThrownBy(() -> useCase.get(CLIENT_ID, SkillId.generate().value()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "SKILL_NOT_FOUND");
  }

  @Test
  @DisplayName("should create from template when template exists")
  void shouldCreateFromTemplateWhenTemplateExists() {
    Skill created = useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");

    assertThat(created.getName()).isEqualTo("Brief Style");
    assertThat(created.getInstructions()).contains("Lead with the direct answer");
    assertThat(created.getAllowedTools()).isEmpty();
  }

  @Test
  @DisplayName("should create localized skill when language is zh")
  void shouldCreateLocalizedSkillWhenLanguageIsZh() {
    Skill created = useCase.createFromTemplate(CLIENT_ID, "brief-style", "zh");

    assertThat(created.getName()).isEqualTo("简洁风格");
    assertThat(created.getInstructions()).contains("先用一两句话给出直接答案");
  }

  @Test
  @DisplayName("should suffix name when create from template conflicts")
  void shouldSuffixNameWhenCreateFromTemplateConflicts() {
    useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");

    Skill duplicate = useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");

    assertThat(duplicate.getName()).isEqualTo("Brief Style (2)");
  }

  @Test
  @DisplayName("should disable skill when set enabled false")
  void shouldDisableSkillWhenSetEnabledFalse() {
    Skill seeded =
        repository.seed(Skill.create(CLIENT_ID, "Brief Style", "", "Instructions", List.of()));

    Skill updated = useCase.setEnabled(CLIENT_ID, seeded.getId().value(), false);

    assertThat(updated.isEnabled()).isFalse();
  }
}
