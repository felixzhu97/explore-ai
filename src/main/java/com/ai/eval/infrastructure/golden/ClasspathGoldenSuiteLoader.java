package com.ai.eval.infrastructure.golden;

import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.repository.GoldenSuiteRepository;
import com.ai.eval.domain.vo.GoldenEvalDomain;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads OpenAI Evals-style JSONL golden suites from {@code classpath:eval/golden/*.jsonl}.
 */
@Repository
public class ClasspathGoldenSuiteLoader implements GoldenSuiteRepository {

    private static final String PATTERN = "classpath:eval/golden/*.jsonl";

    private final ObjectMapper objectMapper;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public ClasspathGoldenSuiteLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<GoldenEvalCase> loadAll() {
        try {
            Resource[] resources = resolver.getResources(PATTERN);
            List<GoldenEvalCase> cases = new ArrayList<>();
            for (Resource resource : resources) {
                cases.addAll(readResource(resource));
            }
            return List.copyOf(cases);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load golden suites from " + PATTERN, ex);
        }
    }

    @Override
    public List<GoldenEvalCase> loadByDomains(List<GoldenEvalDomain> domains) {
        if (domains == null || domains.isEmpty()) {
            return loadAll();
        }
        return loadAll().stream().filter(c -> domains.contains(c.domain())).toList();
    }

    List<GoldenEvalCase> readResource(Resource resource) throws IOException {
        List<GoldenEvalCase> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.strip();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    cases.add(parseLine(trimmed, lineNo, resource.getFilename()));
                } catch (RuntimeException ex) {
                    throw new IllegalStateException(
                            "Invalid golden JSONL in " + resource.getFilename() + " line " + lineNo + ": " + ex.getMessage(),
                            ex);
                }
            }
        }
        return cases;
    }

    GoldenEvalCase parseLine(String json, int lineNo, String filename) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        if (!root.hasNonNull("input") || !root.has("ideal") || root.get("ideal").isNull()) {
            throw new IllegalArgumentException("input and ideal are required");
        }

        String id = root.hasNonNull("id")
                ? root.get("id").asText()
                : (filename == null ? "case" : filename) + "-" + lineNo;
        String userText = extractInput(root.get("input"));
        List<String> ideal = extractIdeal(root.get("ideal"));

        JsonNode metadata = root.path("metadata");
        GoldenEvalDomain domain = parseDomain(metadata.path("domain").asText(null));
        boolean toolsEnabled = metadata.path("tools_enabled").asBoolean(false);
        List<String> contexts = readStringList(metadata.get("contexts"));
        List<String> documentIds = readStringList(metadata.get("document_ids"));
        List<String> fixtureKeys = readStringList(metadata.get("fixture_keys"));

        return new GoldenEvalCase(id, domain, userText, ideal, toolsEnabled, contexts, documentIds, fixtureKeys);
    }

    private static String extractInput(JsonNode inputNode) {
        if (inputNode.isTextual()) {
            return inputNode.asText();
        }
        if (inputNode.isArray()) {
            for (JsonNode message : inputNode) {
                if ("user".equalsIgnoreCase(message.path("role").asText())
                        && message.hasNonNull("content")) {
                    return message.get("content").asText();
                }
            }
            throw new IllegalArgumentException("chat-format input must include a user message");
        }
        throw new IllegalArgumentException("input must be a string or chat message array");
    }

    private static List<String> extractIdeal(JsonNode idealNode) {
        if (idealNode.isTextual()) {
            String text = idealNode.asText();
            if (text.isBlank()) {
                throw new IllegalArgumentException("ideal must not be blank");
            }
            return List.of(text);
        }
        if (idealNode.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : idealNode) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            }
            if (values.isEmpty()) {
                throw new IllegalArgumentException("ideal array must contain non-blank strings");
            }
            return List.copyOf(values);
        }
        throw new IllegalArgumentException("ideal must be a string or string array");
    }

    private static GoldenEvalDomain parseDomain(String raw) {
        if (raw == null || raw.isBlank()) {
            return GoldenEvalDomain.CHAT;
        }
        return GoldenEvalDomain.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    private static List<String> readStringList(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        }
        return List.copyOf(values);
    }
}
