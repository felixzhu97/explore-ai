package com.ai.skill.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.skill.controller.dto.CreateSkillFromTemplateRequest;
import com.ai.skill.controller.dto.CreateSkillRequest;
import com.ai.skill.controller.dto.SetSkillEnabledRequest;
import com.ai.skill.controller.dto.SkillResponse;
import com.ai.skill.controller.dto.SkillTemplateResponse;
import com.ai.skill.controller.dto.UpdateSkillRequest;
import com.ai.skill.service.usecase.SkillUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Documentation. */
@RestController
@RequestMapping("/api/skills")
public class SkillController {

  private final OwnerContext ownerContext;

  private final SkillUseCase skillUseCase;

  /** Documentation. */
  public SkillController(SkillUseCase skillUseCase, OwnerContext ownerContext) {
    this.ownerContext = ownerContext;
    this.skillUseCase = skillUseCase;
  }

  /** Documentation. */
  @GetMapping
  public List<SkillResponse> list(HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return skillUseCase.list(clientId).stream().map(SkillResponse::from).toList();
  }

  /** Documentation. */
  @GetMapping("/templates")
  public List<SkillTemplateResponse> listTemplates(
      @RequestParam(value = "lang", required = false) String lang, HttpServletRequest request) {
    String language = resolveLanguage(lang, request);
    return skillUseCase.listTemplates(language).stream().map(SkillTemplateResponse::from).toList();
  }

  /** Documentation. */
  @PostMapping("/from-template")
  public ResponseEntity<SkillResponse> createFromTemplate(
      @Valid @RequestBody CreateSkillFromTemplateRequest body,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    String language = resolveLanguage(lang, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SkillResponse.from(
                skillUseCase.createFromTemplate(clientId, body.templateId(), language)));
  }

  /** Documentation. */
  @GetMapping("/{id}")
  public SkillResponse get(@PathVariable String id, HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return SkillResponse.from(skillUseCase.get(clientId, id));
  }

  /** Documentation. */
  @PostMapping
  public ResponseEntity<SkillResponse> create(
      @Valid @RequestBody CreateSkillRequest body, HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SkillResponse.from(
                skillUseCase.create(
                    clientId,
                    body.name(),
                    body.description(),
                    body.instructions(),
                    body.allowedTools())));
  }

  /** Documentation. */
  @PutMapping("/{id}")
  public SkillResponse update(
      @PathVariable String id,
      @Valid @RequestBody UpdateSkillRequest body,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return SkillResponse.from(
        skillUseCase.update(
            clientId,
            id,
            body.name(),
            body.description(),
            body.instructions(),
            body.allowedTools()));
  }

  /** Documentation. */
  @PatchMapping("/{id}/enabled")
  public SkillResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetSkillEnabledRequest body,
      HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    return SkillResponse.from(skillUseCase.setEnabled(clientId, id, body.enabled()));
  }

  /** Documentation. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
    String clientId = ownerContext.requireValue(request);
    skillUseCase.delete(clientId, id);
    return ResponseEntity.noContent().build();
  }

  private static String resolveLanguage(String lang, HttpServletRequest request) {
    if (lang != null && !lang.isBlank()) {
      return lang;
    }
    String acceptLanguage = request.getHeader("Accept-Language");
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      return Locale.ENGLISH.getLanguage();
    }
    return acceptLanguage;
  }
}
