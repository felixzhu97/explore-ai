package com.ai.pipeline.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowTemplateCatalog")
class WorkflowTemplateCatalogTest {

    @Test
    @DisplayName("should_listBuiltInTemplates_when_catalogLoaded")
    void should_listBuiltInTemplates_when_catalogLoaded() {
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
    @DisplayName("should_localizeTemplates_when_languageProvided")
    void should_localizeTemplates_when_languageProvided() {
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
    @DisplayName("should_fallbackToEnglish_when_languageUnsupported")
    void should_fallbackToEnglish_when_languageUnsupported() {
        assertThat(WorkflowTemplateCatalog.findById("meetingPrep", "de"))
                .isPresent()
                .get()
                .extracting(WorkflowTemplate::name)
                .isEqualTo("Stakeholder meeting prep");
    }

    @Test
    @DisplayName("should_collectNameAliases_when_templateExists")
    void should_collectNameAliases_when_templateExists() {
        assertThat(WorkflowTemplateCatalog.namesForTemplate("competitiveIntel"))
                .contains("Competitive intelligence", "竞品情报");
    }

    @Test
    @DisplayName("should_returnEmpty_when_idUnknown")
    void should_returnEmpty_when_idUnknown() {
        assertThat(WorkflowTemplateCatalog.findById("missing")).isEmpty();
        assertThat(WorkflowTemplateCatalog.findById(" ")).isEmpty();
    }
}
