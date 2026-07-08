package com.ai.audio.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("should reject null voices")
    void should_reject_null_voices() {
        assertThatThrownBy(() -> new VoiceCatalog(null, List.of("tts-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voices list");
    }

    @Test
    @DisplayName("should return immutable lists")
    void should_return_immutable_lists() {
        List<String> mutableVoices = new ArrayList<>(List.of("alloy"));
        VoiceCatalog catalog = new VoiceCatalog(mutableVoices, List.of("tts-1"));

        mutableVoices.add("echo");

        assertThat(catalog.voices()).containsExactly("alloy");
        assertThatThrownBy(() -> catalog.voices().add("nova"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
