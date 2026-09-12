package com.ai.audio.domain.model;

import java.util.Arrays;
import java.util.Objects;

/** Documentation. */
public class SynthesizedAudio {

  private final byte[] data;
  private final String mediaType;

  private SynthesizedAudio(byte[] data, String mediaType) {
    this.data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
    this.mediaType = mediaType == null || mediaType.isBlank() ? "audio/mpeg" : mediaType;
  }

  /** Documentation. */
  public static SynthesizedAudio create(byte[] data) {
    return new SynthesizedAudio(data, "audio/mpeg");
  }

  /** Documentation. */
  public static SynthesizedAudio create(byte[] data, String mediaType) {
    return new SynthesizedAudio(data, mediaType);
  }

  /** Documentation. */
  public static SynthesizedAudio empty() {
    return new SynthesizedAudio(new byte[0], "audio/mpeg");
  }

  public boolean isEmpty() {
    return data.length == 0;
  }

  /** Documentation. */
  public int sizeInBytes() {
    return data.length;
  }

  /** Documentation. */
  public byte[] data() {
    return Arrays.copyOf(data, data.length);
  }

  /** Documentation. */
  public String mediaType() {
    return mediaType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SynthesizedAudio that)) {
      return false;
    }
    return Arrays.equals(data, that.data) && Objects.equals(mediaType, that.mediaType);
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(data) + Objects.hashCode(mediaType);
  }
}
