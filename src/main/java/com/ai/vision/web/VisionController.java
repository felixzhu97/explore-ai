package com.ai.vision.web;

import com.ai.vision.application.usecase.VisionAnalysisUseCase;
import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.web.dto.CaptionResponse;
import com.ai.vision.web.dto.DetectResponse;
import com.ai.vision.web.dto.OcrResponse;
import com.ai.vision.web.dto.VisionHealthResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/vision")
@ConditionalOnProperty(prefix = "launchdarkly.bootstrap", name = "module-vision", havingValue = "true", matchIfMissing = true)
public class VisionController {

    private final VisionAnalysisUseCase visionAnalysisUseCase;

    public VisionController(VisionAnalysisUseCase visionAnalysisUseCase) {
        this.visionAnalysisUseCase = visionAnalysisUseCase;
    }

    @PostMapping("/caption")
    public CaptionResponse caption(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        validateFile(file);
        return visionAnalysisUseCase.caption(file);
    }

    @PostMapping("/detect")
    public DetectResponse detect(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        validateFile(file);
        return visionAnalysisUseCase.detect(file);
    }

    @PostMapping("/ocr")
    public OcrResponse ocr(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        validateFile(file);
        return visionAnalysisUseCase.ocr(file);
    }

    @GetMapping("/health")
    public VisionHealthResponse health() {
        return visionAnalysisUseCase.health();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VisionInvalidFileException("Image file is required");
        }
    }
}
