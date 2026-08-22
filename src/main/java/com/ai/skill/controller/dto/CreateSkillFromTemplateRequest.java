package com.ai.skill.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** Documentation. */
public record CreateSkillFromTemplateRequest(@NotBlank String templateId) {}
