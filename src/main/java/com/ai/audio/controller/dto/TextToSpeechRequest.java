package com.ai.audio.controller.dto;

/** Request to synthesize speech from text. */
public record TextToSpeechRequest(String text, String voice, Double speed, String outputFormat) {}
