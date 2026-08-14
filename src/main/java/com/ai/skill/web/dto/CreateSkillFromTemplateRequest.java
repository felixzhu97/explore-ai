package com.ai.skill.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record CreateSkillFromTemplateRequest(@NotBlank String templateId) {}
