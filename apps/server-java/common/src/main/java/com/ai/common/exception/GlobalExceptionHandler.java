package com.ai.common.exception;

import com.ai.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(400, e.getMessage()));
	}

	@ExceptionHandler(WebExchangeBindException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(WebExchangeBindException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.badRequest()
				.body(ApiResponse.error(400, message));
	}

	@ExceptionHandler(ServerWebInputException.class)
	public ResponseEntity<ApiResponse<Void>> handleInputException(ServerWebInputException e) {
		return ResponseEntity.badRequest()
				.body(ApiResponse.error(400, e.getReason()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error(500, "Internal server error"));
	}
}
