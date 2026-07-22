package com.ai.agent.infrastructure.prompt;

import com.ai.agent.domain.model.AgentDefinition;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentPromptCatalog")
class AgentPromptCatalogTest {

    @Test
    @DisplayName("should_appendSharedStyle_when_defaultAgentsLoaded")
    void should_appendSharedStyle_when_defaultAgentsLoaded() {
        AgentPromptCatalog catalog = new AgentPromptCatalog(new PromptTemplates());

        List<AgentDefinition> agents = catalog.defaultAgents();

        assertThat(agents).isNotEmpty();
        assertThat(agents).allSatisfy(agent -> {
            assertThat(agent.systemPrompt()).contains("minimal and high-value");
            assertThat(agent.systemPrompt()).contains("No decorative emoji");
        });
        assertThat(agents.stream().map(a -> a.type().value()))
                .contains("supervisor", "research", "weather", "vectordb", "analyst");
    }
}
