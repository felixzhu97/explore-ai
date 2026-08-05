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
    @DisplayName("should_createTemplate_when_nameAvailable")
    void should_createTemplate_when_nameAvailable() {
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
    @DisplayName("should_throw_when_nameConflictOnCreate")
    void should_throw_when_nameConflictOnCreate() {
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
    @DisplayName("should_throw_when_getMissing")
    void should_throw_when_getMissing() {
        assertThatThrownBy(() -> useCase.get(CLIENT_ID, WorkflowTemplateId.generate().value()))
                .isInstanceOf(WorkflowTemplateNotFoundException.class);
    }

    @Test
    @DisplayName("should_createFromTemplate_when_templateExists")
    void should_createFromTemplate_when_templateExists() {
        SavedWorkflowTemplate created = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

        assertThat(created.getName()).isEqualTo("Competitive intelligence");
        assertThat(created.getAgentTypes()).containsExactly("research", "analyst");
        assertThat(created.getSourceTemplateId()).isEqualTo("competitiveIntel");
        assertThat(created.getBriefPrompt()).contains("Competitive Intelligence Brief");
    }

    @Test
    @DisplayName("should_createLocalizedTemplate_when_languageIsZh")
    void should_createLocalizedTemplate_when_languageIsZh() {
        SavedWorkflowTemplate created = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "zh");

        assertThat(created.getName()).isEqualTo("竞品情报");
        assertThat(created.getBriefPrompt()).contains("竞品情报简报");
    }

    @Test
    @DisplayName("should_suffixName_when_createFromTemplateConflicts")
    void should_suffixName_when_createFromTemplateConflicts() {
        useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

        SavedWorkflowTemplate duplicate = useCase.createFromTemplate(CLIENT_ID, "competitiveIntel", "en");

        assertThat(duplicate.getName()).isEqualTo("Competitive intelligence (2)");
    }

    @Test
    @DisplayName("should_disableTemplate_when_setEnabledFalse")
    void should_disableTemplate_when_setEnabledFalse() {
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
