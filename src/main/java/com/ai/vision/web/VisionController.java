package com.ai.vision.web;

import com.ai.vision.application.usecase.VisionAnalysisUseCase;
import com.ai.vision.web.dto.CaptionResponse;
import com.ai.vision.web.dto.DetectResponse;
import com.ai.vision.web.dto.OcrResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/vision")
public class VisionController {

    private static final Logger log = LoggerFactory.getLogger(VisionController.class);

    private final VisionAnalysisUseCase visionAnalysisUseCase;

    public VisionController(VisionAnalysisUseCase visionAnalysisUseCase) {
        this.visionAnalysisUseCase = visionAnalysisUseCase;
    }

    @PostMapping("/caption")
    public ResponseEntity<CaptionResponse> caption(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(visionAnalysisUseCase.caption(file));
        } catch (Exception e) {
            log.error("Caption analysis failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/detect")
    public ResponseEntity<DetectResponse> detect(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(visionAnalysisUseCase.detect(file));
        } catch (Exception e) {
            log.error("Object detection failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/ocr")
    public ResponseEntity<OcrResponse> ocr(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(visionAnalysisUseCase.ocr(file));
        } catch (Exception e) {
            log.error("OCR analysis failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
