package com.ai.common.infrastructure.skills;

import com.ai.common.domain.vo.AgentSkill;
import com.ai.common.infrastructure.config.AgentSkillsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springaicommunity.agent.tools.SkillsTool;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AgentSkillsRuntime {
    private static final Logger log = LoggerFactory.getLogger(AgentSkillsRuntime.class);
    private final boolean enabled;
    private final List<AgentSkill> skills;
    private final ToolCallback skillToolCallback;

    AgentSkillsRuntime(AgentSkillsProperties agentProperties, AgentSkillLoader skillLoader) {
        this.enabled = agentProperties.getSkills().isEnabled();
        this.skills = skillLoader.loadEnabledSkills();
        this.skillToolCallback = buildSkillToolCallback(this.skills);
        if (enabled) log.info("Agent Skills runtime enabled with {} skill(s)", skills.size());
    }

    public boolean enabled() { return enabled && !skills.isEmpty(); }
    public List<AgentSkill> skills() { return skills; }
    public Optional<ToolCallback> skillToolCallback() { return (!enabled() || skillToolCallback == null) ? Optional.empty() : Optional.of(skillToolCallback); }

    public String augmentSystemPrompt(String basePrompt) {
        if (!enabled() || skills.isEmpty()) return basePrompt;
        String catalog = skills.stream().map(skill -> "- " + skill.name() + ": " + skill.description()).collect(Collectors.joining("\n"));
        return basePrompt + "\n\n## Agent Skills\nUse the Skill tool when a configured skill matches the user request.\nAvailable skills:\n" + catalog;
    }

    private static ToolCallback buildSkillToolCallback(List<AgentSkill> skills) {
        if (skills.isEmpty()) return null;
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        SkillsTool.Builder builder = SkillsTool.builder();
        for (AgentSkill skill : skills) {
            try { builder.addSkillsResource(resourceLoader.getResource(toSpringResourceLocation(skill.resourceLocation()))); }
            catch (Exception ex) { log.warn("Skipping Agent Skill tool registration for '{}': {}", skill.name(), ex.getMessage()); }
        }
        try { return builder.build(); }
        catch (Exception ex) { log.warn("Failed to build Agent Skills tool callback: {}", ex.getMessage()); return null; }
    }

    private static String toSpringResourceLocation(String resourceUri) {
        if (resourceUri.startsWith("file:")) return resourceUri;
        int jarMarker = resourceUri.indexOf("!/");
        if (resourceUri.startsWith("jar:") && jarMarker > 0) return resourceUri.substring(0, jarMarker + 2) + normalizeClasspathEntry(resourceUri.substring(jarMarker + 2));
        return normalizeClasspathEntry(resourceUri);
    }

    private static String normalizeClasspathEntry(String entry) {
        while (entry.startsWith("/")) entry = entry.substring(1);
        return "classpath:" + entry;
    }
}
