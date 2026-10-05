package com.ai.skill.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.skill.controller.dto.CreateSkillFromTemplateRequest;
import com.ai.skill.controller.dto.CreateSkillRequest;
import com.ai.skill.controller.dto.SetSkillEnabledRequest;
import com.ai.skill.controller.dto.SkillResponse;
import com.ai.skill.controller.dto.SkillTemplateResponse;
import com.ai.skill.controller.dto.UpdateSkillRequest;
import com.ai.skill.service.SkillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

  private final SkillService skillService;
  private final OwnerContext ownerContext;

  @GetMapping
  public List<SkillResponse> list(HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return skillService.list(ownerKey).stream().map(SkillResponse::from).toList();
  }

  @GetMapping("/templates")
  public List<SkillTemplateResponse> listTemplates(
      @RequestParam(value = "lang", required = false) String lang, HttpServletRequest request) {
    String language = resolveLanguage(lang, request);
    return skillService.listTemplates(language).stream().map(SkillTemplateResponse::from).toList();
  }

  @PostMapping("/from-template")
  public ResponseEntity<SkillResponse> createFromTemplate(
      @Valid @RequestBody CreateSkillFromTemplateRequest body,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = resolveLanguage(lang, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SkillResponse.from(
                skillService.createFromTemplate(ownerKey, body.templateId(), language)));
  }

  @GetMapping("/{id}")
  public SkillResponse get(@PathVariable String id, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return SkillResponse.from(skillService.get(ownerKey, id));
  }

  @PostMapping
  public ResponseEntity<SkillResponse> create(
      @Valid @RequestBody CreateSkillRequest body, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            SkillResponse.from(
                skillService.create(
                    ownerKey,
                    body.name(),
                    body.description(),
                    body.instructions(),
                    body.allowedTools())));
  }

  @PutMapping("/{id}")
  public SkillResponse update(
      @PathVariable String id,
      @Valid @RequestBody UpdateSkillRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return SkillResponse.from(
        skillService.update(
            ownerKey,
            id,
            body.name(),
            body.description(),
            body.instructions(),
            body.allowedTools()));
  }

  @PatchMapping("/{id}/enabled")
  public SkillResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetSkillEnabledRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return SkillResponse.from(skillService.setEnabled(ownerKey, id, body.enabled()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    skillService.delete(ownerKey, id);
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
