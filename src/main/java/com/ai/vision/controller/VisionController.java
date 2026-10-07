package com.ai.vision.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.common.exception.DomainException;
import com.ai.vision.controller.dto.CaptionResponse;
import com.ai.vision.controller.dto.DetectResponse;
import com.ai.vision.controller.dto.OcrResponse;
import com.ai.vision.controller.dto.VisionHealthResponse;
import com.ai.vision.service.VisionAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/vision")
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-vision",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
public class VisionController {

  private final VisionAnalysisService visionAnalysisService;
  private final OwnerContext ownerContext;

  /** Returns the vision module health. */
  @GetMapping("/health")
  public VisionHealthResponse getHealth() {
    return visionAnalysisService.getHealth();
  }

  /** Describes the uploaded image. */
  @PostMapping("/caption")
  public CaptionResponse captionImage(
      @RequestParam(value = "file", required = false) MultipartFile file,
      HttpServletRequest request)
      throws IOException {
    validateFile(file);
    return visionAnalysisService.captionImage(file, ownerContext.require(request));
  }

  /** Detects objects in the uploaded image. */
  @PostMapping("/detect")
  public DetectResponse detect(
      @RequestParam(value = "file", required = false) MultipartFile file,
      HttpServletRequest request)
      throws IOException {
    validateFile(file);
    return visionAnalysisService.detect(file, ownerContext.require(request));
  }

  /** Reads the text in the uploaded image. */
  @PostMapping("/ocr")
  public OcrResponse recognizeText(
      @RequestParam(value = "file", required = false) MultipartFile file,
      HttpServletRequest request)
      throws IOException {
    validateFile(file);
    return visionAnalysisService.recognizeText(file, ownerContext.require(request));
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw DomainException.invalid("INVALID_FILE", "Image file is required");
    }
  }
}
