package com.ai.vision;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.ai.openai.base-url=https://api.deepseek.com",
        "spring.datasource.url=jdbc:h2:mem:vision-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "VISION_MODELS_READY", matches = "true")
@DisplayName("VisionFunctionalVerificationIT")
class VisionFunctionalVerificationIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.ai.vision.domain.repository.ImageCaptioner captioner;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("should_return_health_status")
    void should_return_health_status() throws Exception {
        mockMvc.perform(get("/api/vision/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.providers.ocr").exists());
    }

    @Test
    @DisplayName("should_reject_empty_file_with_structured_error")
    void should_reject_empty_file_with_structured_error() throws Exception {
        mockMvc.perform(multipart("/api/vision/ocr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_FILE"));
    }

    @Test
    @DisplayName("should_ocr_fixture_image")
    void should_ocr_fixture_image() throws Exception {
        MockMultipartFile file = fixture("vision/fixtures/ocr-hello.png", "image/png");
        mockMvc.perform(multipart("/api/vision/ocr").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullText").exists())
                .andExpect(jsonPath("$.processingTimeMs").isNumber());
    }

    @Test
    @DisplayName("should_detect_objects_in_fixture_image")
    void should_detect_objects_in_fixture_image() throws Exception {
        MockMultipartFile file = fixture("vision/fixtures/street-person.jpg", "image/jpeg");
        mockMvc.perform(multipart("/api/vision/detect").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detections").isArray())
                .andExpect(jsonPath("$.processingTimeMs").isNumber());
    }

    @Test
    @DisplayName("should_caption_fixture_image")
    void should_caption_fixture_image() throws Exception {
        Assumptions.assumeTrue(captioner.isAvailable(), "BLIP captioner is not available");
        MockMultipartFile file = fixture("vision/fixtures/landscape.jpg", "image/jpeg");
        mockMvc.perform(multipart("/api/vision/caption").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").isNotEmpty())
                .andExpect(jsonPath("$.processingTimeMs").isNumber());
    }

    private MockMultipartFile fixture(String path, String contentType) throws Exception {
        byte[] bytes = new ClassPathResource(path).getContentAsByteArray();
        String filename = path.substring(path.lastIndexOf('/') + 1);
        return new MockMultipartFile("file", filename, contentType, bytes);
    }
}
