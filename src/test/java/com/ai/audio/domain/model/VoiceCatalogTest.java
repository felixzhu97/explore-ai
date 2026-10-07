package com.ai.audio.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VoiceCatalog")
class VoiceCatalogTest {

  @Test
  @DisplayName("should contain default voices and models")
  void shouldContainDefaultVoicesAndModels() {
    VoiceCatalog catalog = VoiceCatalog.createDefaultCatalog();

    assertThat(catalog.containsVoice("alloy")).isTrue();
    assertThat(catalog.containsModel("tts-1")).isTrue();
    assertThat(catalog.getDefaultVoice()).isEqualTo("alloy");
  }

  @Test
  @DisplayName("should reject null voices")
  void shouldRejectNullVoices() {
    assertThatThrownBy(() -> new VoiceCatalog(null, List.of("tts-1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Voices list");
  }

  @Test
  @DisplayName("should return immutable lists")
  void shouldReturnImmutableLists() {
    List<String> mutableVoices = new ArrayList<>(List.of("alloy"));
    VoiceCatalog catalog = new VoiceCatalog(mutableVoices, List.of("tts-1"));

    mutableVoices.add("echo");

    assertThat(catalog.getVoices()).containsExactly("alloy");
    assertThatThrownBy(() -> catalog.getVoices().add("nova"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("should return unknown voice info for blank voice id")
  void shouldReturnUnknownVoiceInfoForBlankVoiceId() {
    VoiceCatalog catalog = new VoiceCatalog(List.of("alloy", ""), List.of("tts-1"));

    List<VoiceInfo> infos = catalog.listVoices();

    assertThat(infos.get(1).getId()).isEqualTo("unknown");
    assertThat(infos.get(1).getName()).isEqualTo("Unknown");
  }
}
