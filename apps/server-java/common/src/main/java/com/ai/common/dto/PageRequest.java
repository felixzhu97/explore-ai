package com.ai.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record PageRequest(
		Integer page,
		Integer size
) {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	public PageRequest {
		if (page == null || page < 0) {
			page = DEFAULT_PAGE;
		}
		if (size == null || size <= 0) {
			size = DEFAULT_SIZE;
		}
		if (size > MAX_SIZE) {
			size = MAX_SIZE;
		}
	}

	@JsonIgnore
	public int offset() {
		return page * size;
	}

	@JsonIgnore
	public int limit() {
		return size;
	}
}
