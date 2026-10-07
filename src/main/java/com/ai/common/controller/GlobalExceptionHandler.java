package com.ai.common.controller;

import com.ai.audio.domain.exception.TtsProviderNotConfiguredException;
import com.ai.automation.domain.exception.AutomationLimitExceededException;
import com.ai.automation.domain.exception.AutomationScheduleNotFoundException;
import com.ai.chat.domain.exception.ChatSessionNotFoundException;
import com.ai.common.controller.dto.ErrorResponse;
import com.ai.common.domain.exception.AiServiceException;
import com.ai.image.domain.exception.ImageProviderNotConfiguredException;
import com.ai.image.domain.exception.InvalidImagePromptException;
import com.ai.pipeline.domain.exception.PipelineTemplateNameConflictException;
import com.ai.pipeline.domain.exception.PipelineTemplateNotFoundException;
import com.ai.pipeline.domain.exception.SavedAgentNotFoundException;
import com.ai.pipeline.domain.exception.SavedAgentTypeConflictException;
import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.exception.DocumentProcessingException;
import com.ai.rag.domain.exception.RagServiceException;
import com.ai.skill.domain.exception.SkillNameConflictException;
import com.ai.skill.domain.exception.SkillNotFoundException;
import com.ai.vision.domain.exception.VisionInvalidFileException;
import com.ai.vision.domain.exception.VisionOcrException;
import com.ai.vision.domain.exception.VisionProviderUnavailableException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps domain, validation, and infrastructure exceptions to HTTP statuses and error bodies. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Returns 404 when a chat session does not exist. */
  @ExceptionHandler(ChatSessionNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSessionNotFound(ChatSessionNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("Session not found", "SESSION_NOT_FOUND"));
  }

  /** Returns 401 when the request has no Client Identity. */
  @ExceptionHandler(ClientIdentityRequiredException.class)
  public ResponseEntity<ErrorResponse> handleClientIdentityRequired(
      ClientIdentityRequiredException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of("Client identity required", "CLIENT_IDENTITY_REQUIRED"));
  }

  /** Returns 404 when a skill does not exist. */
  @ExceptionHandler(SkillNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSkillNotFound(SkillNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "SKILL_NOT_FOUND"));
  }

  /** Returns 409 when a skill name is already taken. */
  @ExceptionHandler(SkillNameConflictException.class)
  public ResponseEntity<ErrorResponse> handleSkillNameConflict(SkillNameConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(e.getMessage(), "SKILL_NAME_CONFLICT"));
  }

  /** Returns 404 when a pipeline template does not exist. */
  @ExceptionHandler(PipelineTemplateNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePipelineTemplateNotFound(
      PipelineTemplateNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "PIPELINE_TEMPLATE_NOT_FOUND"));
  }

  /** Returns 409 when a pipeline template name is already taken. */
  @ExceptionHandler(PipelineTemplateNameConflictException.class)
  public ResponseEntity<ErrorResponse> handlePipelineTemplateNameConflict(
      PipelineTemplateNameConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(e.getMessage(), "PIPELINE_TEMPLATE_NAME_CONFLICT"));
  }

  /** Returns 404 when a saved agent does not exist. */
  @ExceptionHandler(SavedAgentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleSavedAgentNotFound(SavedAgentNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "SAVED_AGENT_NOT_FOUND"));
  }

  /** Returns 404 when an automation schedule does not exist. */
  @ExceptionHandler(AutomationScheduleNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAutomationScheduleNotFound(
      AutomationScheduleNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "AUTOMATION_SCHEDULE_NOT_FOUND"));
  }

  /** Returns 429 when the automation schedule limit is reached. */
  @ExceptionHandler(AutomationLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleAutomationLimitExceeded(
      AutomationLimitExceededException e) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(ErrorResponse.of(e.getMessage(), "AUTOMATION_LIMIT_EXCEEDED"));
  }

  /** Returns 409 when a saved agent of that type already exists. */
  @ExceptionHandler(SavedAgentTypeConflictException.class)
  public ResponseEntity<ErrorResponse> handleSavedAgentTypeConflict(
      SavedAgentTypeConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(e.getMessage(), "SAVED_AGENT_TYPE_CONFLICT"));
  }

  /** Returns 503 when the AI provider fails. */
  @ExceptionHandler(AiServiceException.class)
  public ResponseEntity<ErrorResponse> handleAiServiceError(AiServiceException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of("AI service error: " + e.getMessage(), e.getErrorCode()));
  }

  /** Returns 404 when a document does not exist. */
  @ExceptionHandler(DocumentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(e.getMessage(), "DOCUMENT_NOT_FOUND"));
  }

  /** Returns 422 when a document cannot be processed. */
  @ExceptionHandler(DocumentProcessingException.class)
  public ResponseEntity<ErrorResponse> handleDocumentProcessing(DocumentProcessingException e) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(ErrorResponse.of(e.getMessage(), "DOCUMENT_UNREADABLE"));
  }

  /** Returns 503 when the vision provider is unavailable. */
  @ExceptionHandler(VisionProviderUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleVisionProviderUnavailable(
      VisionProviderUnavailableException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of(e.getMessage(), "VISION_PROVIDER_UNAVAILABLE"));
  }

  /** Returns 400 for an invalid vision file. */
  @ExceptionHandler(VisionInvalidFileException.class)
  public ResponseEntity<ErrorResponse> handleVisionInvalidFile(VisionInvalidFileException e) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage(), "INVALID_FILE"));
  }

  /** Returns 500 when OCR fails. */
  @ExceptionHandler(VisionOcrException.class)
  public ResponseEntity<ErrorResponse> handleVisionOcrError(VisionOcrException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of(e.getMessage(), "OCR_FAILED"));
  }

  /** Returns 500 when the RAG service fails. */
  @ExceptionHandler(RagServiceException.class)
  public ResponseEntity<ErrorResponse> handleRagServiceError(RagServiceException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("RAG service error: " + e.getMessage(), "RAG_SERVICE_ERROR"));
  }

  /** Returns 503 when the image provider is not configured. */
  @ExceptionHandler(ImageProviderNotConfiguredException.class)
  public ResponseEntity<ErrorResponse> handleImageProviderNotConfigured(
      ImageProviderNotConfiguredException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of(e.getMessage(), "IMAGE_PROVIDER_NOT_CONFIGURED"));
  }

  /** Returns 400 for an invalid image prompt. */
  @ExceptionHandler(InvalidImagePromptException.class)
  public ResponseEntity<ErrorResponse> handleInvalidImagePrompt(InvalidImagePromptException e) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(e.getMessage(), "INVALID_IMAGE_PROMPT"));
  }

  /** Returns 503 when text-to-speech is not configured. */
  @ExceptionHandler(TtsProviderNotConfiguredException.class)
  public ResponseEntity<ErrorResponse> handleTtsProviderNotConfigured(
      TtsProviderNotConfiguredException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of(e.getMessage(), "TTS_PROVIDER_NOT_CONFIGURED"));
  }

  /** Returns 400 with the invalid request body fields. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(ErrorResponse.of(message, "VALIDATION_ERROR"));
  }

  /** Returns 400 with the invalid request parameters. */
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleMethodValidationError(
      HandlerMethodValidationException e) {
    String message =
        e.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                result.getMethodParameter().getParameterName()
                                    + ": "
                                    + error.getDefaultMessage()))
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(ErrorResponse.of(message, "VALIDATION_ERROR"));
  }

  /** Returns 400 for an invalid argument. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage(), "BAD_REQUEST"));
  }

  /** Returns 413 when an upload is too large. */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException e) {
    String limit = formatUploadLimit(e.getMaxUploadSize());
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(
            ErrorResponse.of(
                "Uploaded file exceeds the maximum allowed size of " + limit, "FILE_TOO_LARGE"));
  }

  /** Returns 503 when the database fails. */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDataAccessError(DataAccessException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            ErrorResponse.of(
                "Chat memory storage is temporarily unavailable", "CHAT_MEMORY_ERROR"));
  }

  /** Returns 404 for an unknown endpoint. */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("No endpoint at " + e.getResourcePath(), "NOT_FOUND"));
  }

  /** Returns 405 with the allowed methods. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException e) {
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
    Set<HttpMethod> supported = e.getSupportedHttpMethods();
    if (supported != null && !supported.isEmpty()) {
      builder.allow(supported.toArray(HttpMethod[]::new));
    }
    return builder.body(
        ErrorResponse.of("Method " + e.getMethod() + " is not supported", "METHOD_NOT_ALLOWED"));
  }

  /** Returns 415 for an unsupported content type. */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(
            ErrorResponse.of(
                "Content type " + e.getContentType() + " is not supported",
                "UNSUPPORTED_MEDIA_TYPE"));
  }

  /** Returns 400 when the request body cannot be read. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of("Request body is missing or malformed", "BAD_REQUEST"));
  }

  /** Returns 400 when a required parameter is missing. */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException e) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(
                "Required parameter '" + e.getParameterName() + "' is missing", "BAD_REQUEST"));
  }

  /** Returns 400 when a parameter has the wrong type. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.of(
                "Parameter '" + e.getName() + "' has an invalid value", "BAD_REQUEST"));
  }

  /** Returns 500 for any other error. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
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
