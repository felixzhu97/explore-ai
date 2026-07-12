package com.ai.vision.infrastructure.config;

import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(VisionModelProperties.class)
public class VisionConfig {

    static {
        configureJnaLibraryPath(Path.of("/opt/homebrew/lib"));
        configureJnaLibraryPath(Path.of("/usr/local/lib"));
    }

    @PostConstruct
    void configureNativeLibraries() {
        configureJnaLibraryPath(Path.of("/opt/homebrew/lib"));
        configureJnaLibraryPath(Path.of("/usr/local/lib"));
    }

    private static void configureJnaLibraryPath(Path libraryPath) {
        if (!Files.isDirectory(libraryPath)) {
            return;
        }
        String existing = System.getProperty("jna.library.path", "");
        String path = libraryPath.toString();
        if (existing.isBlank()) {
            System.setProperty("jna.library.path", path);
            return;
        }
        if (!existing.contains(path)) {
            System.setProperty("jna.library.path", existing + File.pathSeparator + path);
        }
    }
}

@Configuration
class TesseractConfig {

    @Bean
    ITesseract tesseract(VisionModelProperties properties) {
        Tesseract tesseract = new Tesseract();
        Path tessdataPath = Path.of(properties.getOcr().getTessdataPath());
        if (Files.isDirectory(tessdataPath)) {
            tesseract.setDatapath(tessdataPath.toAbsolutePath().toString());
        }
        tesseract.setLanguage(properties.getOcr().getLanguages());
        tesseract.setPageSegMode(properties.getOcr().getPageSegMode());
        return tesseract;
    }
}
