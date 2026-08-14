package com.ai.skill.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Documentation. */
public record UpdateSkillRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    @NotBlank String instructions,
    List<String> allowedTools) {}
