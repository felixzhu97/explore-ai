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
@DisplayName("K8sResourceAuditTool")
class K8sResourceAuditToolTest {

    @Mock
    private ToolRegistryPort registry;

    private K8sResourceAuditTool tool;

    @BeforeEach
    void setUp() {
        tool = new K8sResourceAuditTool(registry);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("k8s_resource_audit");
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
        @DisplayName("should aggregate results from sub-tools")
        void shouldAggregateResultsFromSubTools() {
            when(registry.invoke(any())).thenReturn(ToolResult.success("mock result"));

            ToolResult result = tool.execute(new ToolInvocation("k8s_resource_audit", Map.of()));

            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("get_cpu");
            assertThat(result.content()).contains("get_memory");
            assertThat(result.content()).contains("rag_search");
            verify(registry, times(5)).invoke(any());
        }
    }
}
