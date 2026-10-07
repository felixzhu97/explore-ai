package com.ai.vision.infra.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.exception.DomainException;
import com.ai.vision.infra.config.VisionModelProperties;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tess4jOcrEngine")
class Tess4jOcrEngineTest {

  private static final BufferedImage IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

  @Mock private ITesseract tesseract;

  @TempDir private Path tessdata;

  @Test
  @DisplayName(
      "should return the trimmed text when tesseract and the english tessdata are installed")
  void shouldReturnTheTrimmedTextWhenTesseractAndTheEnglishTessdataAreInstalled() throws Exception {
    Files.createFile(tessdata.resolve("eng.traineddata"));
    Tess4jOcrEngine engine = new Tess4jOcrEngine(tesseract, propertiesWith(tessdata));
    assumeTrue(engine.isAvailable(), "Tesseract native library is not installed");
    when(tesseract.doOCR(IMAGE)).thenReturn("  Hello \n");

    assertThat(engine.extractText(IMAGE).text()).isEqualTo("Hello");
  }

  @Test
  @DisplayName("should refuse to read text when the english tessdata is missing")
  void shouldRefuseToReadTextWhenTheEnglishTessdataIsMissing() {
    Tess4jOcrEngine engine = new Tess4jOcrEngine(tesseract, propertiesWith(tessdata));

    assertThat(engine.isAvailable()).isFalse();
    assertThatThrownBy(() -> engine.extractText(IMAGE))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "VISION_PROVIDER_UNAVAILABLE");
    verifyNoInteractions(tesseract);
  }

  private static VisionModelProperties propertiesWith(Path tessdataPath) {
    VisionModelProperties properties = new VisionModelProperties();
    properties.getOcr().setTessdataPath(tessdataPath.toString());
    return properties;
  }
}
