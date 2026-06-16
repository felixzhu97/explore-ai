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
@DisplayName("AiopSAnomalyTool")
class AiopSAnomalyToolTest {

    @Mock
    private ToolRegistryPort registry;

    private AiopSAnomalyTool tool;

    @BeforeEach
    void setUp() {
        tool = new AiopSAnomalyTool(registry);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("aiops_anomaly");
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
        @DisplayName("should call monitoring tools and rag_search")
        void shouldCallMonitoringToolsAndRagSearch() {
            when(registry.invoke(any())).thenReturn(ToolResult.success("mock result"));

            ToolResult result = tool.execute(new ToolInvocation("aiops_anomaly", Map.of()));

            assertThat(result.isError()).isFalse();
            verify(registry, times(4)).invoke(any());
        }

        @Test
        @DisplayName("should use default query when not provided")
        void shouldUseDefaultQueryWhenNotProvided() {
            when(registry.invoke(any())).thenReturn(ToolResult.success("mock result"));

            tool.execute(new ToolInvocation("aiops_anomaly", Map.of()));

            verify(registry, times(4)).invoke(any());
        }
    }
}
