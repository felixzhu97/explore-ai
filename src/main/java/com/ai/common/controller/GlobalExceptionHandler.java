package com.ai.common.controller;

import com.ai.common.controller.dto.ErrorResponse;
import com.ai.common.exception.DomainException;
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

  /** Returns the status for the kind of domain error, with its code in the body. */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleDomainError(DomainException e) {
    return ResponseEntity.status(statusOf(e.getKind()))
        .body(ErrorResponse.createResponse(e.getMessage(), e.getCode()));
  }

  /** Returns 401 when the request has no Client Identity. */
  @ExceptionHandler(ClientIdentityRequiredException.class)
  public ResponseEntity<ErrorResponse> handleClientIdentityRequired(
      ClientIdentityRequiredException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.createResponse("Client identity required", "CLIENT_IDENTITY_REQUIRED"));
  }

  /** Returns 400 with the invalid request body fields. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest()
        .body(ErrorResponse.createResponse(message, "VALIDATION_ERROR"));
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
    return ResponseEntity.badRequest()
        .body(ErrorResponse.createResponse(message, "VALIDATION_ERROR"));
  }

  /** Returns 400 for an invalid argument. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.createResponse(e.getMessage(), "BAD_REQUEST"));
  }

  /** Returns 413 when an upload is too large. */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException e) {
    String limit = formatUploadLimit(e.getMaxUploadSize());
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(
            ErrorResponse.createResponse(
                "Uploaded file exceeds the maximum allowed size of " + limit, "FILE_TOO_LARGE"));
  }

  /** Returns 503 when the database fails. */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ErrorResponse> handleDataAccessError(DataAccessException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            ErrorResponse.createResponse(
                "Chat memory storage is temporarily unavailable", "CHAT_MEMORY_ERROR"));
  }

  /** Returns 404 for an unknown endpoint. */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.createResponse("No endpoint at " + e.getResourcePath(), "NOT_FOUND"));
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
        ErrorResponse.createResponse(
            "Method " + e.getMethod() + " is not supported", "METHOD_NOT_ALLOWED"));
  }

  /** Returns 415 for an unsupported content type. */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(
            ErrorResponse.createResponse(
                "Content type " + e.getContentType() + " is not supported",
                "UNSUPPORTED_MEDIA_TYPE"));
  }

  /** Returns 400 when the request body cannot be read. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.createResponse("Request body is missing or malformed", "BAD_REQUEST"));
  }

  /** Returns 400 when a required parameter is missing. */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException e) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.createResponse(
                "Required parameter '" + e.getParameterName() + "' is missing", "BAD_REQUEST"));
  }

  /** Returns 400 when a parameter has the wrong type. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e) {
    return ResponseEntity.badRequest()
        .body(
            ErrorResponse.createResponse(
                "Parameter '" + e.getName() + "' has an invalid value", "BAD_REQUEST"));
  }

  /** Returns 500 for any other error. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.createResponse("An unexpected error occurred", "INTERNAL_ERROR"));
  }

  /** Maps each kind of domain error to its HTTP status. */
  public static HttpStatus statusOf(DomainException.Kind kind) {
    return switch (kind) {
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case INVALID -> HttpStatus.BAD_REQUEST;
      case LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
      case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_CONTENT;
      case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
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
