package com.ai.skill.application.usecase;

import com.ai.skill.domain.exception.SkillNameConflictException;
import com.ai.skill.domain.exception.SkillNotFoundException;
import com.ai.skill.domain.model.Skill;
import com.ai.skill.domain.repository.SkillRepository;
import com.ai.skill.domain.vo.SkillId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SkillUseCaseImpl")
class SkillUseCaseImplTest {

    private static final String CLIENT_ID = "client-1";

    private FakeSkillRepository repository;
    private SkillUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeSkillRepository();
        useCase = new SkillUseCaseImpl(repository);
    }

    @Test
    @DisplayName("should_createSkill_when_nameAvailable")
    void should_createSkill_when_nameAvailable() {
        Skill created = useCase.create(
                CLIENT_ID,
                "Brief Style",
                "Short answers",
                "Be concise.",
                List.of("Read"));

        assertThat(created.getName()).isEqualTo("Brief Style");
        assertThat(repository.saveCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_throw_when_nameConflictOnCreate")
    void should_throw_when_nameConflictOnCreate() {
        repository.seed(Skill.create(CLIENT_ID, "Brief Style", "", "Instructions", List.of()));

        assertThatThrownBy(() -> useCase.create(
                CLIENT_ID,
                "Brief Style",
                "",
                "Other",
                List.of()))
                .isInstanceOf(SkillNameConflictException.class);
    }

    @Test
    @DisplayName("should_returnSkill_when_getExisting")
    void should_returnSkill_when_getExisting() {
        Skill seeded = repository.seed(Skill.create(CLIENT_ID, "Brief Style", "", "Instructions", List.of()));

        Skill found = useCase.get(CLIENT_ID, seeded.getId().value());

        assertThat(found.getId()).isEqualTo(seeded.getId());
    }

    @Test
    @DisplayName("should_throw_when_getMissing")
    void should_throw_when_getMissing() {
        assertThatThrownBy(() -> useCase.get(CLIENT_ID, SkillId.generate().value()))
                .isInstanceOf(SkillNotFoundException.class);
    }

    @Test
    @DisplayName("should_createFromTemplate_when_templateExists")
    void should_createFromTemplate_when_templateExists() {
        Skill created = useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");

        assertThat(created.getName()).isEqualTo("Brief Style");
        assertThat(created.getInstructions()).contains("Lead with the direct answer");
        assertThat(created.getAllowedTools()).isEmpty();
    }

    @Test
    @DisplayName("should_createLocalizedSkill_when_languageIsZh")
    void should_createLocalizedSkill_when_languageIsZh() {
        Skill created = useCase.createFromTemplate(CLIENT_ID, "brief-style", "zh");

        assertThat(created.getName()).isEqualTo("简洁风格");
        assertThat(created.getInstructions()).contains("先用一两句话给出直接答案");
    }

    @Test
    @DisplayName("should_suffixName_when_createFromTemplateConflicts")
    void should_suffixName_when_createFromTemplateConflicts() {
        useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");

        Skill duplicate = useCase.createFromTemplate(CLIENT_ID, "brief-style", "en");

        assertThat(duplicate.getName()).isEqualTo("Brief Style (2)");
    }

    @Test
    @DisplayName("should_disableSkill_when_setEnabledFalse")
    void should_disableSkill_when_setEnabledFalse() {
        Skill seeded = repository.seed(Skill.create(CLIENT_ID, "Brief Style", "", "Instructions", List.of()));

        Skill updated = useCase.setEnabled(CLIENT_ID, seeded.getId().value(), false);

        assertThat(updated.isEnabled()).isFalse();
    }

    private static final class FakeSkillRepository implements SkillRepository {

        private final List<Skill> skills = new ArrayList<>();
        private int saveCount;

        Skill seed(Skill skill) {
            skills.add(skill);
            return skill;
        }

        int saveCount() {
            return saveCount;
        }

        @Override
        public Skill save(Skill skill) {
            saveCount++;
            skills.removeIf(existing -> existing.getId().equals(skill.getId()));
            skills.add(skill);
            return skill;
        }

        @Override
        public Optional<Skill> findByIdAndClientId(SkillId id, String clientId) {
            return skills.stream()
                    .filter(skill -> skill.getId().equals(id) && skill.getClientId().equals(clientId))
                    .findFirst();
        }

        @Override
        public List<Skill> findAllByClientId(String clientId) {
            return skills.stream()
                    .filter(skill -> skill.getClientId().equals(clientId))
                    .toList();
        }

        @Override
        public List<Skill> findEnabledByClientIdAndIds(String clientId, List<SkillId> ids) {
            return skills.stream()
                    .filter(skill -> skill.getClientId().equals(clientId))
                    .filter(Skill::isEnabled)
                    .filter(skill -> ids.contains(skill.getId()))
                    .toList();
        }

        @Override
        public void deleteByIdAndClientId(SkillId id, String clientId) {
            skills.removeIf(skill -> skill.getId().equals(id) && skill.getClientId().equals(clientId));
        }

        @Override
        public boolean existsByClientIdAndNameIgnoringId(String clientId, String name, SkillId excludeId) {
            return skills.stream()
                    .filter(skill -> skill.getClientId().equals(clientId))
                    .filter(skill -> skill.getName().equals(name))
                    .anyMatch(skill -> excludeId == null || !skill.getId().equals(excludeId));
        }
    }
}
