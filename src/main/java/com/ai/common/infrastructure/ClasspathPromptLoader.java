package com.ai.common.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Loads prompt text from {@code classpath:prompts/**}.
 * Variable substitution uses Spring AI {@link PromptTemplate} via
 * {@link ClasspathPromptTemplate}; static fragments with JSON braces are loaded
 * and joined here without rendering.
 */
public final class ClasspathPromptLoader {

    private static final String ROOT = "prompts/";

    private ClasspathPromptLoader() {}

    public static String load(String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");
        String path = relativePath.startsWith(ROOT) ? relativePath : ROOT + relativePath;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClasspathPromptLoader.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing prompt resource: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load prompt resource: " + path, e);
        }
    }

    public static String joinSections(String... sections) {
        StringBuilder sb = new StringBuilder();
        for (String section : sections) {
            if (section == null || section.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(section.strip());
        }
        return sb.toString();
    }

}
