package com.ai.audio.domain.model;

import java.util.Arrays;
import java.util.Objects;

/** Immutable synthesized speech audio bytes with their media type, defaulting to MP3. */
public class SynthesizedAudio {

  private final byte[] data;
  private final String mediaType;

  private SynthesizedAudio(byte[] data, String mediaType) {
    this.data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
    this.mediaType = mediaType == null || mediaType.isBlank() ? "audio/mpeg" : mediaType;
  }

  /** Creates MP3 audio from the bytes. */
  public static SynthesizedAudio createAudio(byte[] data) {
    return new SynthesizedAudio(data, "audio/mpeg");
  }

  /** Creates audio from the bytes and media type. */
  public static SynthesizedAudio createAudio(byte[] data, String mediaType) {
    return new SynthesizedAudio(data, mediaType);
  }

  /** Creates empty MP3 audio. */
  public static SynthesizedAudio createEmptyAudio() {
    return new SynthesizedAudio(new byte[0], "audio/mpeg");
  }

  /** Tells whether the audio has no bytes. */
  public boolean isEmpty() {
    return data.length == 0;
  }

  /** Returns the audio size in bytes. */
  public int getSizeInBytes() {
    return data.length;
  }

  /** Returns a copy of the audio bytes. */
  public byte[] copyBytes() {
    return Arrays.copyOf(data, data.length);
  }

  /** Returns the audio media type. */
  public String getMediaType() {
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
