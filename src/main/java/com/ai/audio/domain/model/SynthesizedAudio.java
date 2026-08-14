package com.ai.audio.domain.model;

import java.util.Arrays;

/** Documentation. */
public class SynthesizedAudio {

  private final byte[] data;

  private SynthesizedAudio(byte[] data) {
    this.data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
  }

  /** Documentation. */
  public static SynthesizedAudio create(byte[] data) {
    return new SynthesizedAudio(data);
  }

  /** Documentation. */
  public static SynthesizedAudio empty() {
    return new SynthesizedAudio(new byte[0]);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SynthesizedAudio that)) {
      return false;
    }
    return Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }
}
