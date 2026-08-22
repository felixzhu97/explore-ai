package com.ai.vision.controller;

import com.ai.vision.controller.dto.CaptionResponse;
import com.ai.vision.controller.dto.DetectResponse;
import com.ai.vision.controller.dto.OcrResponse;
import com.ai.vision.controller.dto.VisionHealthResponse;
import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.service.usecase.VisionAnalysisUseCase;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Documentation. */
@RestController
@RequestMapping("/api/vision")
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-vision",
    havingValue = "true",
    matchIfMissing = true)
public class VisionController {

  private final VisionAnalysisUseCase visionAnalysisUseCase;

  /** Documentation. */
  public VisionController(VisionAnalysisUseCase visionAnalysisUseCase) {
    this.visionAnalysisUseCase = visionAnalysisUseCase;
  }

  /** Documentation. */
  @PostMapping("/caption")
  public CaptionResponse caption(@RequestParam(value = "file", required = false) MultipartFile file)
      throws IOException {
    validateFile(file);
    return visionAnalysisUseCase.caption(file);
  }

  /** Documentation. */
  @PostMapping("/detect")
  public DetectResponse detect(@RequestParam(value = "file", required = false) MultipartFile file)
      throws IOException {
    validateFile(file);
    return visionAnalysisUseCase.detect(file);
  }

  /** Documentation. */
  @PostMapping("/ocr")
  public OcrResponse ocr(@RequestParam(value = "file", required = false) MultipartFile file)
      throws IOException {
    validateFile(file);
    return visionAnalysisUseCase.ocr(file);
  }

  /** Documentation. */
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
