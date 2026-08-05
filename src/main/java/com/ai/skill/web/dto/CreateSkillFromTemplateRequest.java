package com.ai.skill.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSkillFromTemplateRequest(@NotBlank String templateId) {}
