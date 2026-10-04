package com.ai.audio.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Request to synthesize speech from text. */
public record TextToSpeechRequest(
    String text, String voice, Double speed, @JsonAlias("output_format") String outputFormat) {}
