package com.ai.skill.domain.model;

import com.ai.skill.domain.vo.SkillId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Skill")
class SkillTest {

    @Test
    @DisplayName("should_createEnabledSkill_when_createCalled")
    void should_createEnabledSkill_when_createCalled() {
        Skill skill = Skill.create(
                "client-1",
                "Brief Style",
                "Short answers",
                "Be concise.",
                List.of("Read"));

        assertThat(skill.getId()).isNotNull();
        assertThat(skill.getClientId()).isEqualTo("client-1");
        assertThat(skill.getName()).isEqualTo("Brief Style");
        assertThat(skill.getDescription()).isEqualTo("Short answers");
        assertThat(skill.getInstructions()).isEqualTo("Be concise.");
        assertThat(skill.getAllowedTools()).containsExactly("Read");
        assertThat(skill.isEnabled()).isTrue();
        assertThat(skill.getCreatedAt()).isNotNull();
        assertThat(skill.getUpdatedAt()).isEqualTo(skill.getCreatedAt());
    }

    @Test
    @DisplayName("should_updateFields_when_updateCalled")
    void should_updateFields_when_updateCalled() {
        Skill skill = Skill.create("client-1", "Old", "Old desc", "Old instructions", List.of());
        Instant beforeUpdate = skill.getUpdatedAt();

        skill.update("New", "New desc", "New instructions", List.of("Search"));

        assertThat(skill.getName()).isEqualTo("New");
        assertThat(skill.getDescription()).isEqualTo("New desc");
        assertThat(skill.getInstructions()).isEqualTo("New instructions");
        assertThat(skill.getAllowedTools()).containsExactly("Search");
        assertThat(skill.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("should_disableSkill_when_disableCalled")
    void should_disableSkill_when_disableCalled() {
        Skill skill = Skill.create("client-1", "Name", "", "Instructions", List.of());

        skill.disable();

        assertThat(skill.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should_restoreSkill_when_restoreCalled")
    void should_restoreSkill_when_restoreCalled() {
        SkillId id = SkillId.generate();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");

        Skill skill = Skill.restore(
                id,
                "client-1",
                "Name",
                "Description",
                "Instructions",
                List.of("Read"),
                false,
                createdAt,
                updatedAt);

        assertThat(skill.getId()).isEqualTo(id);
        assertThat(skill.isEnabled()).isFalse();
        assertThat(skill.getCreatedAt()).isEqualTo(createdAt);
        assertThat(skill.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("should_throw_when_nameBlank")
    void should_throw_when_nameBlank() {
        assertThatThrownBy(() -> Skill.create("client-1", "  ", "", "Instructions", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }
}
