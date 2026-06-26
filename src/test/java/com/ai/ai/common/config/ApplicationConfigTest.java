package com.ai.ai.common.config;

import com.ai.ai.domain.model.ChatSession;
import com.ai.ai.domain.service.LanguageDetectionService;
import com.ai.ai.domain.repository.ChatSessionRepository;
import com.ai.ai.infrastructure.store.InMemoryChatSessionRepository;
import com.ai.shared.config.ApplicationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplicationConfig Tests")
class ApplicationConfigTest {

    private final AnnotationConfigApplicationContext context;

    ApplicationConfigTest() {
        this.context = new AnnotationConfigApplicationContext(ApplicationConfig.class);
    }

    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {

        @Test
        @DisplayName("should create ChatSessionRepository bean")
        void shouldCreateChatSessionRepositoryBean() {
            // When
            ChatSessionRepository repository = context.getBean(ChatSessionRepository.class);

            // Then
            assertThat(repository).isNotNull();
            assertThat(repository).isInstanceOf(InMemoryChatSessionRepository.class);
        }

        @Test
        @DisplayName("should create LanguageDetectionService bean")
        void shouldCreateLanguageDetectionServiceBean() {
            // When
            LanguageDetectionService service = context.getBean(LanguageDetectionService.class);

            // Then
            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("should create single instance of ChatSessionRepository")
        void shouldCreateSingleInstance_ofChatSessionRepository() {
            // When
            ChatSessionRepository repository1 = context.getBean(ChatSessionRepository.class);
            ChatSessionRepository repository2 = context.getBean(ChatSessionRepository.class);

            // Then
            assertThat(repository1).isSameAs(repository2);
        }

        @Test
        @DisplayName("should create single instance of LanguageDetectionService")
        void shouldCreateSingleInstance_ofLanguageDetectionService() {
            // When
            LanguageDetectionService service1 = context.getBean(LanguageDetectionService.class);
            LanguageDetectionService service2 = context.getBean(LanguageDetectionService.class);

            // Then
            assertThat(service1).isSameAs(service2);
        }
    }

    @Nested
    @DisplayName("Bean Type Tests")
    class BeanTypeTests {

        @Test
        @DisplayName("should return InMemoryChatSessionRepository type")
        void shouldReturnInMemoryChatSessionRepositoryType() {
            // When
            ChatSessionRepository repository = context.getBean(ChatSessionRepository.class);

            // Then
            assertThat(repository).isInstanceOf(InMemoryChatSessionRepository.class);
        }
    }

    @Nested
    @DisplayName("Repository Functionality Tests")
    class RepositoryFunctionalityTests {

        @Test
        @DisplayName("should have working repository from bean")
        void shouldHaveWorkingRepository_fromBean() {
            // Given
            ChatSessionRepository repository = context.getBean(ChatSessionRepository.class);
            var session = ChatSession.create("Test Session");

            // When
            repository.save(session);
            var found = repository.findById(session.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(session.getId());
        }

        @Test
        @DisplayName("should clear repository between uses")
        void shouldClearRepository_betweenUses() {
            // Given
            ChatSessionRepository repository = context.getBean(ChatSessionRepository.class);
            var session = ChatSession.create("Test Session");
            repository.save(session);

            // When
            if (repository instanceof InMemoryChatSessionRepository inMemoryRepo) {
                inMemoryRepo.clear();
            }

            // Then
            assertThat(repository.findAll()).isEmpty();
        }
    }
}
