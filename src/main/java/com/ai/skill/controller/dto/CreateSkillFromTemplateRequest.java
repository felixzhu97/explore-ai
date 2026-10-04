package com.ai.skill.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSkillFromTemplateRequest(@NotBlank String templateId) {}
