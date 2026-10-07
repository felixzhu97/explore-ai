package com.ai.pipeline.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PipelineTemplateCatalog")
class PipelineTemplateCatalogTest {

  @Test
  @DisplayName("should list built in templates when catalog loaded")
  void shouldListBuiltInTemplatesWhenCatalogLoaded() {
    assertThat(PipelineTemplateCatalog.listAll())
        .extracting(BuiltinPipelineTemplate::id)
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
    assertThat(PipelineTemplateCatalog.findById("competitiveIntel", "zh"))
        .isPresent()
        .get()
        .extracting(BuiltinPipelineTemplate::name)
        .isEqualTo("竞品情报");

    assertThat(PipelineTemplateCatalog.findById("incidentReview", "zh"))
        .isPresent()
        .get()
        .extracting(BuiltinPipelineTemplate::name)
        .isEqualTo("事故复盘");

    assertThat(PipelineTemplateCatalog.findById("policyQa", "en"))
        .isPresent()
        .get()
        .extracting(BuiltinPipelineTemplate::agentTypes)
        .isEqualTo(java.util.List.of("vectordb", "analyst"));
  }

  @Test
  @DisplayName("should fallback to english when language unsupported")
  void shouldFallbackToEnglishWhenLanguageUnsupported() {
    assertThat(PipelineTemplateCatalog.findById("meetingPrep", "de"))
        .isPresent()
        .get()
        .extracting(BuiltinPipelineTemplate::name)
        .isEqualTo("Stakeholder meeting prep");
  }

  @Test
  @DisplayName("should collect name aliases when template exists")
  void shouldCollectNameAliasesWhenTemplateExists() {
    assertThat(PipelineTemplateCatalog.listNamesForTemplate("competitiveIntel"))
        .contains("Competitive intelligence", "竞品情报");
  }

  @Test
  @DisplayName("should return empty when id unknown")
  void shouldReturnEmptyWhenIdUnknown() {
    assertThat(PipelineTemplateCatalog.findById("missing")).isEmpty();
    assertThat(PipelineTemplateCatalog.findById(" ")).isEmpty();
  }
}
