package com.ai.audio.domain.model;

import lombok.Value;

/** Display details of one speech voice. */
@Value
public class VoiceInfo {
  String id;
  String name;
  String language;
  String gender;
}
