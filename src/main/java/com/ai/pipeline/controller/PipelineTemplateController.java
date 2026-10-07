package com.ai.pipeline.controller;

import com.ai.account.controller.OwnerContext;
import com.ai.pipeline.controller.dto.BuiltinPipelineTemplateResponse;
import com.ai.pipeline.controller.dto.CreatePipelineTemplateFromDefinitionRequest;
import com.ai.pipeline.controller.dto.CreatePipelineTemplateRequest;
import com.ai.pipeline.controller.dto.PipelineTemplateResponse;
import com.ai.pipeline.controller.dto.SetPipelineTemplateEnabledRequest;
import com.ai.pipeline.controller.dto.UpdatePipelineTemplateRequest;
import com.ai.pipeline.service.PipelineTemplateService;
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
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
public class PipelineTemplateController {

  private final PipelineTemplateService pipelineTemplateService;
  private final OwnerContext ownerContext;

  /** Lists the built-in pipeline templates. */
  @GetMapping("/template-definitions")
  public List<BuiltinPipelineTemplateResponse> listTemplates(
      @RequestParam(value = "lang", required = false) String lang, HttpServletRequest request) {
    String language = resolveLanguage(lang, request);
    return pipelineTemplateService.listTemplates(language).stream()
        .map(BuiltinPipelineTemplateResponse::createResponse)
        .toList();
  }

  /** Lists the owner's saved pipelines. */
  @GetMapping("/templates")
  public List<PipelineTemplateResponse> listLibrary(HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return pipelineTemplateService.listLibrary(ownerKey).stream()
        .map(PipelineTemplateResponse::createResponse)
        .toList();
  }

  /** Saves a copy of a built-in template. */
  @PostMapping("/templates/from-template")
  public ResponseEntity<PipelineTemplateResponse> createFromTemplate(
      @Valid @RequestBody CreatePipelineTemplateFromDefinitionRequest body,
      @RequestParam(value = "lang", required = false) String lang,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    String language = resolveLanguage(lang, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            PipelineTemplateResponse.createResponse(
                pipelineTemplateService.createFromTemplate(ownerKey, body.templateId(), language)));
  }

  /** Saves a new pipeline. */
  @PostMapping("/templates")
  public ResponseEntity<PipelineTemplateResponse> createTemplate(
      @Valid @RequestBody CreatePipelineTemplateRequest body, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            PipelineTemplateResponse.createResponse(
                pipelineTemplateService.createTemplate(
                    ownerKey,
                    body.name(),
                    body.description(),
                    body.agentTypes(),
                    body.topic(),
                    body.brief(),
                    null)));
  }

  /** Updates a saved pipeline. */
  @PutMapping("/templates/{id}")
  public PipelineTemplateResponse updateTemplate(
      @PathVariable String id,
      @Valid @RequestBody UpdatePipelineTemplateRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return PipelineTemplateResponse.createResponse(
        pipelineTemplateService.updateTemplate(
            ownerKey,
            id,
            body.name(),
            body.description(),
            body.agentTypes(),
            body.topic(),
            body.brief()));
  }

  /** Turns a saved pipeline on or off. */
  @PatchMapping("/templates/{id}/enabled")
  public PipelineTemplateResponse setEnabled(
      @PathVariable String id,
      @Valid @RequestBody SetPipelineTemplateEnabledRequest body,
      HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    return PipelineTemplateResponse.createResponse(
        pipelineTemplateService.setEnabled(ownerKey, id, body.enabled()));
  }

  /** Deletes a saved pipeline. */
  @DeleteMapping("/templates/{id}")
  public ResponseEntity<Void> deleteTemplate(@PathVariable String id, HttpServletRequest request) {
    String ownerKey = ownerContext.requireValue(request);
    pipelineTemplateService.deleteTemplate(ownerKey, id);
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
