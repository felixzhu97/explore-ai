package com.ai.skill.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SkillTemplateCatalog")
class SkillTemplateCatalogTest {

  @Test
  @DisplayName("should list built in templates when catalog loaded")
  void shouldListBuiltInTemplatesWhenCatalogLoaded() {
    assertThat(SkillTemplateCatalog.listAll())
        .extracting(SkillTemplate::id)
        .contains(
            "brief-style",
            "code-review",
            "teaching-tutor",
            "research-analyst",
            "writing-editor",
            "zh-assistant",
            "data-analyst",
            "strategist");
  }

  @Test
  @DisplayName("should localize templates when language provided")
  void shouldLocalizeTemplatesWhenLanguageProvided() {
    assertThat(SkillTemplateCatalog.findById("data-analyst", "zh"))
        .isPresent()
        .get()
        .extracting(SkillTemplate::name, SkillTemplate::description)
        .containsExactly("数据分析专家", "每次回答都要提炼数据要点，并尽可能用图表展示。");

    assertThat(SkillTemplateCatalog.findById("strategist", "ja"))
        .isPresent()
        .get()
        .extracting(SkillTemplate::name)
        .isEqualTo("ストラテジスト");
  }

  @Test
  @DisplayName("should fallback to english when language unsupported")
  void shouldFallbackToEnglishWhenLanguageUnsupported() {
    assertThat(SkillTemplateCatalog.findById("brief-style", "de"))
        .isPresent()
        .get()
        .extracting(SkillTemplate::name)
        .isEqualTo("Brief Style");
  }

  @Test
  @DisplayName("should collect name aliases when template exists")
  void shouldCollectNameAliasesWhenTemplateExists() {
    assertThat(SkillTemplateCatalog.namesForTemplate("brief-style"))
        .contains("Brief Style", "简洁风格", "簡潔スタイル", "Style concis", "Estilo breve");
  }

  @Test
  @DisplayName("should find template when id exists")
  void shouldFindTemplateWhenIdExists() {
    assertThat(SkillTemplateCatalog.findById("code-review"))
        .isPresent()
        .get()
        .extracting(SkillTemplate::name)
        .isEqualTo("Code Review");
    assertThat(SkillTemplateCatalog.findById("data-analyst"))
        .isPresent()
        .get()
        .extracting(SkillTemplate::name)
        .isEqualTo("Data Analyst");
    assertThat(SkillTemplateCatalog.findById("strategist"))
        .isPresent()
        .get()
        .extracting(SkillTemplate::name)
        .isEqualTo("Strategist");
  }

  @Test
  @DisplayName("should return empty when id unknown")
  void shouldReturnEmptyWhenIdUnknown() {
    assertThat(SkillTemplateCatalog.findById("missing")).isEmpty();
    assertThat(SkillTemplateCatalog.findById(" ")).isEmpty();
  }
}
