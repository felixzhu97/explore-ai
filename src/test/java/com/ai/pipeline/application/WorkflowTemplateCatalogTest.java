package com.ai.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowTemplateCatalog")
class WorkflowTemplateCatalogTest {

  @Test
  @DisplayName("should list built in templates when catalog loaded")
  void shouldListBuiltInTemplatesWhenCatalogLoaded() {
    assertThat(WorkflowTemplateCatalog.listAll())
        .extracting(WorkflowTemplate::id)
        .containsExactly(
            "competitiveIntel",
            "policyQa",
            "vendorDiligence",
            "executiveBrief",
            "meetingPrep",
            "incidentReview",
            "marketScan",
            "customerInsight",
            "riskAssessment");
  }

  @Test
  @DisplayName("should localize templates when language provided")
  void shouldLocalizeTemplatesWhenLanguageProvided() {
    assertThat(WorkflowTemplateCatalog.findById("competitiveIntel", "zh"))
        .isPresent()
        .get()
        .extracting(WorkflowTemplate::name)
        .isEqualTo("竞品情报");

    assertThat(WorkflowTemplateCatalog.findById("incidentReview", "zh"))
        .isPresent()
        .get()
        .extracting(WorkflowTemplate::name)
        .isEqualTo("事故复盘");

    assertThat(WorkflowTemplateCatalog.findById("policyQa", "en"))
        .isPresent()
        .get()
        .extracting(WorkflowTemplate::agentTypes)
        .isEqualTo(java.util.List.of("vectordb", "analyst"));
  }

  @Test
  @DisplayName("should fallback to english when language unsupported")
  void shouldFallbackToEnglishWhenLanguageUnsupported() {
    assertThat(WorkflowTemplateCatalog.findById("meetingPrep", "de"))
        .isPresent()
        .get()
        .extracting(WorkflowTemplate::name)
        .isEqualTo("Stakeholder meeting prep");
  }

  @Test
  @DisplayName("should collect name aliases when template exists")
  void shouldCollectNameAliasesWhenTemplateExists() {
    assertThat(WorkflowTemplateCatalog.namesForTemplate("competitiveIntel"))
        .contains("Competitive intelligence", "竞品情报");
  }

  @Test
  @DisplayName("should return empty when id unknown")
  void shouldReturnEmptyWhenIdUnknown() {
    assertThat(WorkflowTemplateCatalog.findById("missing")).isEmpty();
    assertThat(WorkflowTemplateCatalog.findById(" ")).isEmpty();
  }
}
