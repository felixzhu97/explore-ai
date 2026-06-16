package com.ai.infrastructure.adapter.tool.composite;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LlmoopsPipelineTool")
class LlmoopsPipelineToolTest {

    @Mock
    private ToolRegistryPort registry;

    private LlmoopsPipelineTool tool;

    @BeforeEach
    void setUp() {
        tool = new LlmoopsPipelineTool(registry);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("llmops_pipeline");
        }

        @Test
        @DisplayName("should be composite")
        void shouldBeComposite() {
            assertThat(tool.definition().composite()).isTrue();
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should call rag_search, get_jvm, and get_memory")
        void shouldCallRagSearchGetJvmAndGetMemory() {
            when(registry.invoke(any())).thenReturn(ToolResult.success("mock result"));

            ToolResult result = tool.execute(new ToolInvocation("llmops_pipeline", Map.of()));

            assertThat(result.isError()).isFalse();
            verify(registry, times(3)).invoke(any());
        }
    }
}
