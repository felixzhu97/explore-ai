package com.ai.agent.infrastructure.skills;

import com.ai.agent.domain.vo.AgentSkill;
import com.ai.agent.infrastructure.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AgentSkillLoader {
    private static final Logger log = LoggerFactory.getLogger(AgentSkillLoader.class);
    private static final Pattern SKILL_NAME = Pattern.compile("^[a-z0-9-]{1,64}$");
    private final AgentProperties agentProperties;
    public AgentSkillLoader(AgentProperties agentProperties) { this.agentProperties = agentProperties; }

    public List<AgentSkill> loadEnabledSkills() {
        AgentProperties.Skills config = agentProperties.getSkills();
        if (!config.isEnabled()) return List.of();
        Set<String> allowedIds = config.getIds().stream().map(id -> id.trim().toLowerCase(Locale.ROOT)).filter(id -> !id.isBlank()).collect(Collectors.toUnmodifiableSet());
        if (allowedIds.isEmpty()) { log.info("Agent Skills enabled but no skill ids configured; skipping load"); return List.of(); }
        Map<String, AgentSkill> loaded = new LinkedHashMap<>();
        for (Resource resource : discoverSkillFiles(config.getResourceLocation())) {
            try {
                parseSkill(resource).ifPresent(skill -> {
                    if (!allowedIds.contains(skill.name())) return;
                    if (loaded.containsKey(skill.name())) { log.warn("Duplicate Agent Skill '{}'; keeping first match", skill.name()); return; }
                    loaded.put(skill.name(), skill);
                });
            } catch (Exception ex) { log.warn("Skipping invalid Agent Skill at {}: {}", resource, ex.getMessage()); }
        }
        for (String configuredId : allowedIds) {
            if (!loaded.containsKey(configuredId)) log.warn("Configured Agent Skill '{}' was not loaded (missing or invalid)", configuredId);
        }
        return List.copyOf(loaded.values());
    }

    private Resource[] discoverSkillFiles(String resourceLocation) {
        String pattern = normalizeResourceLocation(resourceLocation) + "**/SKILL.md";
        try { return new PathMatchingResourcePatternResolver().getResources(pattern); }
        catch (IOException ex) { log.warn("Failed to scan Agent Skills at {}: {}", pattern, ex.getMessage()); return new Resource[0]; }
    }

    static String normalizeResourceLocation(String resourceLocation) {
        String location = resourceLocation == null || resourceLocation.isBlank() ? "classpath:agent/skills/" : resourceLocation.trim();
        return location.endsWith("/") ? location : location + "/";
    }

    static Optional<AgentSkill> parseSkill(Resource resource) throws IOException {
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!content.startsWith("---")) return Optional.empty();
        int end = content.indexOf("---", 3);
        if (end < 0) return Optional.empty();
        Map<String, String> frontmatter = parseFrontmatter(content.substring(3, end));
        String name = frontmatter.getOrDefault("name", "").trim().toLowerCase(Locale.ROOT);
        String description = frontmatter.getOrDefault("description", "").trim();
        if (name.isBlank() || description.isBlank() || !SKILL_NAME.matcher(name).matches()) return Optional.empty();
        return Optional.of(new AgentSkill(name, description, parseAllowedTools(frontmatter.get("allowed-tools")), content.substring(end + 3).trim(), resourceLocationFor(resource)));
    }

    private static Map<String, String> parseFrontmatter(String yaml) {
        Map<String, String> values = new LinkedHashMap<>();
        String currentKey = null; StringBuilder currentValue = new StringBuilder();
        for (String rawLine : yaml.split("\n")) {
            String line = rawLine.stripTrailing(); if (line.isBlank()) continue;
            int colon = line.indexOf(':');
            if (colon > 0 && !line.startsWith(" ")) {
                if (currentKey != null) values.put(currentKey, currentValue.toString().trim());
                currentKey = line.substring(0, colon).trim();
                currentValue = new StringBuilder(line.substring(colon + 1).trim());
            } else if (currentKey != null) {
                if (!currentValue.isEmpty()) currentValue.append(' ');
                currentValue.append(line.trim());
            }
        }
        if (currentKey != null) values.put(currentKey, currentValue.toString().trim());
        return values;
    }

    private static List<String> parseAllowedTools(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> tools = new ArrayList<>();
        for (String part : raw.split(",")) { String tool = part.trim(); if (!tool.isBlank()) tools.add(tool); }
        return List.copyOf(tools);
    }

    private static String resourceLocationFor(Resource resource) throws IOException {
        String uri = resource.getURI().toString();
        int skillMarker = uri.lastIndexOf("/SKILL.md");
        return skillMarker >= 0 ? uri.substring(0, skillMarker) : uri;
    }
}
