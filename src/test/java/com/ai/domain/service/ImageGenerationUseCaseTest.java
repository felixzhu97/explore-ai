package com.ai.domain.service;

import com.ai.ai.application.usecase.SpringAiImageGenerationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageGenerationUseCase Tests")
class ImageGenerationUseCaseTest {

    @Mock
    private org.springframework.ai.image.ImageModel imageModel;

    private SpringAiImageGenerationUseCase service;

    @BeforeEach
    void setUp() {
        service = new SpringAiImageGenerationUseCase(imageModel);
    }

    @Nested
    @DisplayName("getAvailableModels")
    class GetAvailableModelsTests {

        @Test
        @DisplayName("should return available models")
        void shouldReturnAvailableModels() {
            List<String> models = service.getAvailableModels();

            assertThat(models).containsExactly("dall-e-3", "dall-e-2");
        }
    }

    @Nested
    @DisplayName("getAvailableSizes")
    class GetAvailableSizesTests {

        @Test
        @DisplayName("should return available sizes")
        void shouldReturnAvailableSizes() {
            List<String> sizes = service.getAvailableSizes();

            assertThat(sizes).containsExactly("1024x1024", "1024x1792", "1792x1024");
        }
    }

    @Nested
    @DisplayName("getAvailableQualities")
    class GetAvailableQualitiesTests {

        @Test
        @DisplayName("should return available qualities")
        void shouldReturnAvailableQualities() {
            List<String> qualities = service.getAvailableQualities();

            assertThat(qualities).containsExactly("standard", "hd");
        }
    }
}
