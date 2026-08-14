package com.ai.common.web;

import com.ai.audio.domain.exception.TtsProviderNotConfiguredException;
import com.ai.automation.domain.exception.AutomationLimitExceededException;
import com.ai.automation.domain.exception.AutomationScheduleNotFoundException;
import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.common.domain.exception.AiServiceException;
import com.ai.common.util.LogSanitizer;
import com.ai.common.web.dto.ErrorResponse;
import com.ai.image.domain.exception.ImageProviderNotConfiguredException;
import com.ai.pipeline.domain.exception.SavedAgentNotFoundException;
import com.ai.pipeline.domain.exception.SavedAgentTypeConflictException;
import com.ai.pipeline.domain.exception.WorkflowTemplateNameConflictException;
import com.ai.pipeline.domain.exception.WorkflowTemplateNotFoundException;
import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.exception.RagServiceException;
import com.ai.skill.domain.exception.SkillNameConflictException;
import com.ai.skill.domain.exception.SkillNotFoundException;
import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.domain.exception.VisionOcrException;
import com.ai.vision.domain.exception.VisionProviderUnavailableException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Documentation. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Documentation. */
  @ExceptionHandler(ChatSessionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSessionNotFound(ChatSessionNotFoundException e) {
    log.warn("Session not found: {}", LogSanitizer.fingerprint(e.getSessionId()));
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("Session not found", "SESSION_NOT_FOUND"));
  }

  /** Documentation. */
  @ExceptionHandler(ClientIdentityRequiredException.class)
  public ResponseEntity<ErrorResponse> handleClientIdentityRequired(
      ClientIdentityRequiredException e) {
    log.warn("Client identity missing");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of("Client identity required", "CLIENT_IDENTITY_REQUIRED"));
  }

  /** Documentation. */
  @ExceptionHandler(SkillNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSkillNotFound(SkillNotFoundException e) {
    log.warn("Skill not found: {}", e.getSkillId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "SKILL_NOT_FOUND"));
  }

  /** Documentation. */
  @ExceptionHandler(SkillNameConflictException.class)
  public ResponseEntity<ErrorResponse> handleSkillNameConflict(SkillNameConflictException e) {
    log.warn("Skill name conflict: {}", e.getName());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(e.getMessage(), "SKILL_NAME_CONFLICT"));
  }

  /** Documentation. */
  @ExceptionHandler(WorkflowTemplateNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleWorkflowTemplateNotFound(
      WorkflowTemplateNotFoundException e) {
    log.warn("Workflow template not found: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "WORKFLOW_TEMPLATE_NOT_FOUND"));
  }

  /** Documentation. */
  @ExceptionHandler(WorkflowTemplateNameConflictException.class)
  public ResponseEntity<ErrorResponse> handleWorkflowTemplateNameConflict(
      WorkflowTemplateNameConflictException e) {
    log.warn("Workflow template name conflict: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(e.getMessage(), "WORKFLOW_TEMPLATE_NAME_CONFLICT"));
  }

  /** Documentation. */
  @ExceptionHandler(SavedAgentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSavedAgentNotFound(SavedAgentNotFoundException e) {
    log.warn("Saved agent not found: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "SAVED_AGENT_NOT_FOUND"));
  }

  /** Documentation. */
  @ExceptionHandler(AutomationScheduleNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAutomationScheduleNotFound(
      AutomationScheduleNotFoundException e) {
    log.warn("Automation schedule not found: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "AUTOMATION_SCHEDULE_NOT_FOUND"));
  }

  /** Documentation. */
  @ExceptionHandler(AutomationLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleAutomationLimitExceeded(
      AutomationLimitExceededException e) {
    log.warn("Automation limit exceeded: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(ErrorResponse.of(e.getMessage(), "AUTOMATION_LIMIT_EXCEEDED"));
  }

  /** Documentation. */
  @ExceptionHandler(SavedAgentTypeConflictException.class)
  public ResponseEntity<ErrorResponse> handleSavedAgentTypeConflict(
      SavedAgentTypeConflictException e) {
    log.warn("Saved agent type conflict: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(e.getMessage(), "SAVED_AGENT_TYPE_CONFLICT"));
  }

  /** Documentation. */
  @ExceptionHandler(AiServiceException.class)
  public ResponseEntity<ErrorResponse> handleAiServiceError(AiServiceException e) {
    log.error("AI service error: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of("AI service error: " + e.getMessage(), e.getErrorCode()));
  }

  /** Documentation. */
  @ExceptionHandler(DocumentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException e) {
    log.warn("Document not found: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "DOCUMENT_NOT_FOUND"));
  }

  /** Documentation. */
  @ExceptionHandler(VisionProviderUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleVisionProviderUnavailable(
      VisionProviderUnavailableException e) {
    log.warn("Vision provider unavailable [{}]: {}", e.getProvider(), e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of(e.getMessage(), "VISION_PROVIDER_UNAVAILABLE"));
  }

  /** Documentation. */
  @ExceptionHandler(VisionInvalidFileException.class)
  public ResponseEntity<ErrorResponse> handleVisionInvalidFile(VisionInvalidFileException e) {
    log.warn("Invalid vision input: {}", e.getMessage());
    return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage(), "INVALID_FILE"));
  }

  /** Documentation. */
  @ExceptionHandler(VisionOcrException.class)
  public ResponseEntity<ErrorResponse> handleVisionOcrError(VisionOcrException e) {
    log.error("OCR failed: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(e.getMessage(), "OCR_FAILED"));
  }

  /** Documentation. */
  @ExceptionHandler(RagServiceException.class)
  public ResponseEntity<ErrorResponse> handleRagServiceError(RagServiceException e) {
    log.error("RAG service error: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("RAG service error: " + e.getMessage(), "RAG_SERVICE_ERROR"));
  }

  /** Documentation. */
  @ExceptionHandler(ImageProviderNotConfiguredException.class)
  public ResponseEntity<ErrorResponse> handleImageProviderNotConfigured(
      ImageProviderNotConfiguredException e) {
    log.warn("Image provider not configured: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of(e.getMessage(), "IMAGE_PROVIDER_NOT_CONFIGURED"));
  }

  /** Documentation. */
  @ExceptionHandler(TtsProviderNotConfiguredException.class)
  public ResponseEntity<ErrorResponse> handleTtsProviderNotConfigured(
      TtsProviderNotConfiguredException e) {
    log.warn("TTS provider not configured: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of(e.getMessage(), "TTS_PROVIDER_NOT_CONFIGURED"));
  }

  /** Documentation. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    log.warn("Validation error: {}", message);
    return ResponseEntity.badRequest().body(ErrorResponse.of(message, "VALIDATION_ERROR"));
  }

  /** Documentation. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    log.warn("Illegal argument: {}", e.getMessage());
    return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage(), "BAD_REQUEST"));
  }

  /** Documentation. */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException e) {
    log.warn("Upload size exceeded: {}", e.getMessage());
    String limit = formatUploadLimit(e.getMaxUploadSize());
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(
            ErrorResponse.of(
                "Uploaded file exceeds the maximum allowed size of " + limit, "FILE_TOO_LARGE"));
  }

  /** Documentation. */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDataAccessError(DataAccessException e) {
    log.error("Database error during chat operation", e);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            ErrorResponse.of(
                "Chat memory storage is temporarily unavailable", "CHAT_MEMORY_ERROR"));
  }

  /** Documentation. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
    log.error("Unexpected error", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("An unexpected error occurred", "INTERNAL_ERROR"));
  }

  private String formatUploadLimit(long maxUploadSizeBytes) {
    if (maxUploadSizeBytes < 0) {
      return "the configured limit";
    }
    DataSize size = DataSize.ofBytes(maxUploadSizeBytes);
    if (size.toMegabytes() > 0) {
      return size.toMegabytes() + "MB";
    }
    if (size.toKilobytes() > 0) {
      return size.toKilobytes() + "KB";
    }
    return size.toBytes() + "B";
  }
}
