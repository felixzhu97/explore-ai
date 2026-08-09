package com.ai.pipeline.application.usecase;

import com.ai.pipeline.domain.exception.WorkflowTemplateNameConflictException;
import com.ai.pipeline.domain.exception.WorkflowTemplateNotFoundException;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WorkflowTemplateUseCaseImpl")
class WorkflowTemplateUseCaseImplTest {

    private static final String CLIENT_ID = "client-1";

    private FakeWorkflowTemplateRepository repository;
    private WorkflowTemplateUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeWorkflowTemplateRepository();
        useCase = new WorkflowTemplateUseCaseImpl(repository);
    }

    @Test
    @DisplayName("should create template when name available")
    void shouldCreateTemplateWhenNameAvailable() {
        SavedWorkflowTemplate created = useCase.create(
                CLIENT_ID,
                "My flow",
                "desc",
                List.of("research", "analyst"),
                "topic",
                "brief",
                null);

        assertThat(created.getName()).isEqualTo("My flow");
        assertThat(created.getAgentTypes()).containsExactly("research", "analyst");
        assertThat(repository.saveCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should throw when name conflict on create")
    void shouldThrowWhenNameConflictOnCreate() {
        repository.seed(SavedWorkflowTemplate.create(
                CLIENT_ID, "My flow", "", List.of("analyst"), "", "brief", null));

        assertThatThrownBy(() -> useCase.create(
                CLIENT_ID,
                "My flow",
                "",
                List.of("analyst"),
                "",
                "other",
                null))
                .isInstanceOf(WorkflowTemplateNameConflictException.class);
    }

    @Test
    @DisplayName("should throw when get missing")
    void shouldThrowWhenGetMissing() {
        assertThatThrownBy(() -> useCase.get(CLIENT_ID, WorkflowTemplateId.generate().value()))
                .isInstanceOf(WorkflowTemplateNotFoundException.class);
    }

    @Test
    @DisplayName("should create from template when template exists")
    void shouldCreateFromTemplateWhenTemplateExists() {
        SavedWorkflowTemplate created = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

        assertThat(created.getName()).isEqualTo("Competitive intelligence");
        assertThat(created.getAgentTypes()).containsExactly("research", "analyst");
        assertThat(created.getSourceTemplateId()).isEqualTo("competitiveIntel");
        assertThat(created.getBriefPrompt()).contains("Competitive Intelligence Brief");
    }

    @Test
    @DisplayName("should create localized template when language is zh")
    void shouldCreateLocalizedTemplateWhenLanguageIsZh() {
        SavedWorkflowTemplate created = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "zh");

        assertThat(created.getName()).isEqualTo("竞品情报");
        assertThat(created.getBriefPrompt()).contains("竞品情报简报");
    }

    @Test
    @DisplayName("should suffix name when create from template conflicts")
    void shouldSuffixNameWhenCreateFromTemplateConflicts() {
        useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

        SavedWorkflowTemplate duplicate = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

        assertThat(duplicate.getName()).isEqualTo("Competitive intelligence (2)");
    }

    @Test
    @DisplayName("should disable template when set enabled false")
    void shouldDisableTemplateWhenSetEnabledFalse() {
        SavedWorkflowTemplate seeded = repository.seed(SavedWorkflowTemplate.create(
                CLIENT_ID, "My flow", "", List.of("analyst"), "", "brief", null));

        SavedWorkflowTemplate updated = useCase.setEnabled(CLIENT_ID, seeded.getId().value(), false);

        assertThat(updated.isEnabled()).isFalse();
    }

    private static final class FakeWorkflowTemplateRepository implements WorkflowTemplateRepository {

        private final List<SavedWorkflowTemplate> templates = new ArrayList<>();
        private int saveCount;

        SavedWorkflowTemplate seed(SavedWorkflowTemplate template) {
            templates.add(template);
            return template;
        }

        int saveCount() {
            return saveCount;
        }

        @Override
        public SavedWorkflowTemplate save(SavedWorkflowTemplate template) {
            saveCount++;
            templates.removeIf(existing -> existing.getId().equals(template.getId()));
            templates.add(template);
            return template;
        }

        @Override
        public Optional<SavedWorkflowTemplate> findByIdAndClientId(WorkflowTemplateId id, String clientId) {
            return templates.stream()
                    .filter(t -> t.getId().equals(id) && t.getClientId().equals(clientId))
                    .findFirst();
        }

        @Override
        public List<SavedWorkflowTemplate> findAllByClientId(String clientId) {
            return templates.stream().filter(t -> t.getClientId().equals(clientId)).toList();
        }

        @Override
        public void deleteByIdAndClientId(WorkflowTemplateId id, String clientId) {
            templates.removeIf(t -> t.getId().equals(id) && t.getClientId().equals(clientId));
        }

        @Override
        public boolean existsByClientIdAndNameIgnoringId(
                String clientId, String name, WorkflowTemplateId excludeId) {
            return templates.stream()
                    .anyMatch(t -> t.getClientId().equals(clientId)
                            && t.getName().equals(name)
                            && (excludeId == null || !t.getId().equals(excludeId)));
        }
    }
}
