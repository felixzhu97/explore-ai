package com.ai.image.controller.dto;

import java.util.List;

/**
 * Supported image sizes.
 *
 * @param sizes sizes as {@code WIDTHxHEIGHT}, e.g. {@code 1024x1024}
 */
public record ImageSizesResponse(List<String> sizes) {}
