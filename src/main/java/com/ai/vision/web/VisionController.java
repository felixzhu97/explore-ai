package com.ai.vision.web;

import com.ai.vision.application.usecase.VisionAnalysisUseCase;
import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.domain.port.ImageCaptioner;
import com.ai.vision.domain.port.ObjectDetector;
import com.ai.vision.domain.port.OcrEngine;
import com.ai.vision.web.dto.CaptionResponse;
import com.ai.vision.web.dto.DetectResponse;
import com.ai.vision.web.dto.OcrResponse;
import com.ai.vision.web.dto.VisionHealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vision")
public class VisionController {

    private final VisionAnalysisUseCase visionAnalysisUseCase;
    private final ImageCaptioner captioner;
    private final ObjectDetector detector;
    private final OcrEngine ocrEngine;

    public VisionController(
            VisionAnalysisUseCase visionAnalysisUseCase,
            ImageCaptioner captioner,
            ObjectDetector detector,
            OcrEngine ocrEngine) {
        this.visionAnalysisUseCase = visionAnalysisUseCase;
        this.captioner = captioner;
        this.detector = detector;
        this.ocrEngine = ocrEngine;
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
        Map<String, String> providers = new LinkedHashMap<>();
        providers.put("caption", captioner.isAvailable() ? "UP" : "DOWN");
        providers.put("detect", detector.isAvailable() ? "UP" : "DOWN");
        providers.put("ocr", ocrEngine.isAvailable() ? "UP" : "DOWN");
        boolean allUp = captioner.isAvailable() && detector.isAvailable() && ocrEngine.isAvailable();
        return new VisionHealthResponse(allUp ? "UP" : "DEGRADED", providers);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VisionInvalidFileException("Image file is required");
        }
    }
}
