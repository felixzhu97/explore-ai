package com.ai.infrastructure.adapter.tool.composite;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupervisorOverviewTool")
class SupervisorOverviewToolTest {

    @Mock
    private ToolRegistryPort registry;

    private SupervisorOverviewTool tool;

    @BeforeEach
    void setUp() {
        tool = new SupervisorOverviewTool(registry);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("supervisor_overview");
        }

        @Test
        @DisplayName("should be composite")
        void shouldBeComposite() {
            assertThat(tool.definition().composite()).isTrue();
        }

        @Test
        @DisplayName("should have composite category")
        void shouldHaveCompositeCategory() {
            assertThat(tool.definition().category()).isEqualTo("composite");
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should aggregate results from sub-tools")
        void shouldAggregateResultsFromSubTools() {
            when(registry.invoke(any())).thenReturn(ToolResult.success("mock result"));

            ToolResult result = tool.execute(new ToolInvocation("supervisor_overview", Map.of()));

            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("get_cpu");
            assertThat(result.content()).contains("get_memory");
            assertThat(result.content()).contains("get_disk");
            assertThat(result.content()).contains("get_jvm");
            verify(registry, times(4)).invoke(any());
        }

        @Test
        @DisplayName("should include markdown header")
        void shouldIncludeMarkdownHeader() {
            when(registry.invoke(any())).thenReturn(ToolResult.success("data"));

            ToolResult result = tool.execute(new ToolInvocation("supervisor_overview", Map.of()));

            assertThat(result.content()).contains("System Overview");
        }
    }
}
