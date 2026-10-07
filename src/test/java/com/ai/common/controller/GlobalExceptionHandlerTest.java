package com.ai.common.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ai.common.controller.dto.ErrorResponse;
import com.ai.common.exception.DomainException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * GlobalExceptionHandler Unit Tests.
 *
 * <p>Covers every exception handler with arrange, act, assert tests, including edge cases.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Nested
  @DisplayName("NoResourceFoundException")
  class HandleNoResourceFound {

    @Test
    @DisplayName("should return 404 with NOT_FOUND error code when no endpoint matches")
    void shouldReturn404WithNotFoundErrorCodeWhenNoEndpointMatches() {
      NoResourceFoundException exception =
          new NoResourceFoundException(HttpMethod.GET, "/api/text/providers", "api/text/providers");

      ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().errorCode()).isEqualTo("NOT_FOUND");
    }
  }

  @Nested
  @DisplayName("DomainException")
  class HandleDomainError {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "NOT_FOUND, NOT_FOUND",
      "CONFLICT, CONFLICT",
      "INVALID, BAD_REQUEST",
      "LIMIT_EXCEEDED, TOO_MANY_REQUESTS",
      "UNPROCESSABLE, UNPROCESSABLE_CONTENT",
      "UNAVAILABLE, SERVICE_UNAVAILABLE",
      "FAILED, INTERNAL_SERVER_ERROR"
    })
    @DisplayName("should map each kind to its status and keep the error code")
    void shouldMapEachKindToItsStatusAndKeepTheErrorCode(
        DomainException.Kind kind, HttpStatus status) {
      DomainException exception = new DomainException(kind, "SOME_CODE", "Something went wrong");

      ResponseEntity<ErrorResponse> response = handler.handleDomainError(exception);

      assertThat(response.getStatusCode()).isEqualTo(status);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().errorCode()).isEqualTo("SOME_CODE");
      assertThat(response.getBody().message()).isEqualTo("Something went wrong");
    }

    @Test
    @DisplayName("should not reveal the session id when a session is missing")
    void shouldNotRevealTheSessionIdWhenASessionIsMissing() {
      DomainException exception =
          DomainException.notFound("SESSION_NOT_FOUND", "Session not found");

      ResponseEntity<ErrorResponse> response = handler.handleDomainError(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(response.getBody().errorCode()).isEqualTo("SESSION_NOT_FOUND");
      assertThat(response.getBody().message()).isEqualTo("Session not found");
    }

    @Test
    @DisplayName("should return the message without the cause when a provider fails")
    void shouldReturnTheMessageWithoutTheCauseWhenAProviderFails() {
      DomainException exception =
          DomainException.unavailable(
              "AI_SERVICE_ERROR", "Service unavailable", new RuntimeException("Network timeout"));

      ResponseEntity<ErrorResponse> response = handler.handleDomainError(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
      assertThat(response.getBody().message()).isEqualTo("Service unavailable");
    }
  }

  @Nested
  @DisplayName("MethodArgumentNotValidException")
  class HandleValidationError {

    @Test
    @DisplayName("should return 400 with VALIDATION_ERROR error code")
    void shouldReturn400WithValidationErrorCode() {
      BindingResult bindingResult = mock(BindingResult.class);
      when(bindingResult.getFieldErrors())
          .thenReturn(
              java.util.List.of(
                  new FieldError("object", "field1", "must not be null"),
                  new FieldError("object", "field2", "must not be blank")));

      MethodArgumentNotValidException exception =
          new MethodArgumentNotValidException(null, bindingResult);

      ResponseEntity<ErrorResponse> response = handler.handleValidationError(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
      assertThat(response.getBody().message()).contains("field1");
      assertThat(response.getBody().message()).contains("field2");
    }

    @Test
    @DisplayName("should format field errors as comma-separated")
    void shouldFormatFieldErrorsAsCommaSeparated() {
      BindingResult bindingResult = mock(BindingResult.class);
      when(bindingResult.getFieldErrors())
          .thenReturn(
              java.util.List.of(
                  new FieldError("object", "name", "required"),
                  new FieldError("object", "email", "invalid format")));

      MethodArgumentNotValidException exception =
          new MethodArgumentNotValidException(null, bindingResult);

      ResponseEntity<ErrorResponse> response = handler.handleValidationError(exception);

      assertThat(response.getBody().message()).contains("name: required");
      assertThat(response.getBody().message()).contains("email: invalid format");
    }

    @Test
    @DisplayName("should handle exception with empty field errors")
    void shouldHandleExceptionWithEmptyFieldErrors() {
      BindingResult bindingResult = mock(BindingResult.class);
      when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of());

      MethodArgumentNotValidException exception =
          new MethodArgumentNotValidException(null, bindingResult);

      ResponseEntity<ErrorResponse> response = handler.handleValidationError(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
    }
  }

  @Nested
  @DisplayName("IllegalArgumentException")
  class HandleIllegalArgument {

    @Test
    @DisplayName("should return 400 with BAD_REQUEST error code")
    void shouldReturn400WithBadRequestErrorCode() {
      IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

      ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
      assertThat(response.getBody().message()).contains("Invalid parameter value");
    }

    @Test
    @DisplayName("should handle empty message")
    void shouldHandleEmptyMessage() {
      IllegalArgumentException exception = new IllegalArgumentException("");

      ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
    }
  }

  @Nested
  @DisplayName("MaxUploadSizeExceededException")
  class HandleMaxUploadSizeExceeded {

    @Test
    @DisplayName("should return 413 with FILE_TOO_LARGE error code")
    void shouldReturn413WithFileTooLargeErrorCode() {
      MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(52428800L);

      ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().errorCode()).isEqualTo("FILE_TOO_LARGE");
      assertThat(response.getBody().message()).contains("50MB");
    }

    @Test
    @DisplayName("should handle exception with different max size")
    void shouldHandleExceptionWithDifferentMaxSize() {
      MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(1024L);

      ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
      assertThat(response.getBody().errorCode()).isEqualTo("FILE_TOO_LARGE");
    }
  }

  @Nested
  @DisplayName("Unsupported requests")
  class HandleUnsupportedRequests {

    @Test
    @DisplayName("should return 405 with allow header when method is not supported")
    void shouldReturn405WithAllowHeaderWhenMethodIsNotSupported() {
      HttpRequestMethodNotSupportedException exception =
          new HttpRequestMethodNotSupportedException("GET", Set.of("POST"));

      ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
      assertThat(response.getHeaders().getAllow()).containsExactly(HttpMethod.POST);
      assertThat(response.getBody().errorCode()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("should return 415 when content type is not supported")
    void shouldReturn415WhenContentTypeIsNotSupported() {
      HttpMediaTypeNotSupportedException exception =
          new HttpMediaTypeNotSupportedException(
              MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

      ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotSupported(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
      assertThat(response.getBody().errorCode()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    @DisplayName("should return 400 when request body is malformed")
    void shouldReturn400WhenRequestBodyIsMalformed() {
      HttpMessageNotReadableException exception =
          new HttpMessageNotReadableException(
              "JSON parse error", new MockHttpInputMessage(new byte[0]));

      ResponseEntity<ErrorResponse> response = handler.handleMessageNotReadable(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("should return 400 naming the parameter when it is missing")
    void shouldReturn400NamingTheParameterWhenItIsMissing() {
      MissingServletRequestParameterException exception =
          new MissingServletRequestParameterException("file", "MultipartFile");

      ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().message()).contains("file");
    }

    @Test
    @DisplayName("should return 400 naming the parameter when its type does not match")
    void shouldReturn400NamingTheParameterWhenItsTypeDoesNotMatch() {
      MethodArgumentTypeMismatchException exception =
          new MethodArgumentTypeMismatchException("abc", UUID.class, "id", null, null);

      ResponseEntity<ErrorResponse> response = handler.handleArgumentTypeMismatch(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody().message()).contains("id");
    }
  }

  @Nested
  @DisplayName("Generic Exception Handler")
  class HandleGenericException {

    @Test
    @DisplayName("should return 500 with INTERNAL_ERROR error code")
    void shouldReturn500WithInternalErrorCode() {
      Exception exception = new RuntimeException("Unexpected error");

      ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
      assertThat(response.getBody().message()).contains("unexpected error");
    }

    @Test
    @DisplayName("should handle NullPointerException")
    void shouldHandleNullPointerException() {
      NullPointerException exception = new NullPointerException("Cannot invoke method on null");

      ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("should handle exception with cause")
    void shouldHandleExceptionWithCause() {
      Exception exception =
          new RuntimeException("Outer error", new RuntimeException("Inner error"));

      ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
    }
  }
}
