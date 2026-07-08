package com.ai.audio.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VoiceCatalog")
class VoiceCatalogTest {

    @Test
    @DisplayName("should contain default voices and models")
    void should_contain_default_voices_and_models() {
        VoiceCatalog catalog = VoiceCatalog.defaults();

        assertThat(catalog.containsVoice("alloy")).isTrue();
        assertThat(catalog.containsModel("tts-1")).isTrue();
        assertThat(catalog.defaultVoice()).isEqualTo("alloy");
    }
}
