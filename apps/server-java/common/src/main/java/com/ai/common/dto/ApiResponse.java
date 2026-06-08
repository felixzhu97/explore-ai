package com.ai.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
	Integer code,
	String message,
	T data
) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(200, "OK", data);
	}

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(200, message, data);
	}

	public static ApiResponse<Void> error(int code, String message) {
		return new ApiResponse<>(code, message, null);
	}
}
