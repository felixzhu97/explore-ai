package com.ai.tts.domain.model;

public enum OutputFormat {
    MP3("audio/mpeg", "mp3"),
    WAV("audio/wav", "wav"),
    OGG("audio/ogg", "ogg"),
    FLAC("audio/flac", "flac");

    private final String mediaType;
    private final String extension;

    OutputFormat(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String mediaType() {
        return mediaType;
    }

    public String extension() {
        return extension;
    }
}
