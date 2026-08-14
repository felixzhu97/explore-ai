package com.ai.image.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ImageCatalog")
class ImageCatalogTest {

  @Test
  @DisplayName("should return same default instance")
  void shouldReturnSameDefaultInstance() {
    assertThat(ImageCatalog.defaults()).isSameAs(ImageCatalog.defaults());
  }

  @Test
  @DisplayName("should reject null models")
  void shouldRejectNullModels() {
    assertThatThrownBy(() -> new ImageCatalog(null, List.of("1024x1024"), List.of("standard")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Models list");
  }

  @Test
  @DisplayName("should return immutable lists")
  void shouldReturnImmutableLists() {
    List<String> mutableModels = new ArrayList<>(List.of("dall-e-3"));
    ImageCatalog catalog =
        new ImageCatalog(mutableModels, List.of("1024x1024"), List.of("standard"));

    mutableModels.add("dall-e-2");

    assertThat(catalog.models()).containsExactly("dall-e-3");
    assertThatThrownBy(() -> catalog.models().add("dall-e-2"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
