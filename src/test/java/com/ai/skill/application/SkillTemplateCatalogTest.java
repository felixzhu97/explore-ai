package com.ai.skill.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkillTemplateCatalog")
class SkillTemplateCatalogTest {

    @Test
    @DisplayName("should_listBuiltInTemplates_when_catalogLoaded")
    void should_listBuiltInTemplates_when_catalogLoaded() {
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
    @DisplayName("should_localizeTemplates_when_languageProvided")
    void should_localizeTemplates_when_languageProvided() {
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
    @DisplayName("should_fallbackToEnglish_when_languageUnsupported")
    void should_fallbackToEnglish_when_languageUnsupported() {
        assertThat(SkillTemplateCatalog.findById("brief-style", "de"))
                .isPresent()
                .get()
                .extracting(SkillTemplate::name)
                .isEqualTo("Brief Style");
    }

    @Test
    @DisplayName("should_collectNameAliases_when_templateExists")
    void should_collectNameAliases_when_templateExists() {
        assertThat(SkillTemplateCatalog.namesForTemplate("brief-style"))
                .contains("Brief Style", "简洁风格", "簡潔スタイル", "Style concis", "Estilo breve");
    }

    @Test
    @DisplayName("should_findTemplate_when_idExists")
    void should_findTemplate_when_idExists() {
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
    @DisplayName("should_returnEmpty_when_idUnknown")
    void should_returnEmpty_when_idUnknown() {
        assertThat(SkillTemplateCatalog.findById("missing")).isEmpty();
        assertThat(SkillTemplateCatalog.findById(" ")).isEmpty();
    }
}
