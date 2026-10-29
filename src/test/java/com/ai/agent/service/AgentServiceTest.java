package com.ai.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.agent.domain.model.AgentRun;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.pipeline.service.PipelineService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentService")
class AgentServiceTest {

  @Mock private PipelineService pipelineService;
  @InjectMocks private AgentService agentService;

  @Test
  @DisplayName("should list agents via PipelineService")
  void shouldListAgents() {
    when(pipelineService.listAgents("owner", "en"))
        .thenReturn(
            List.of(
                AgentDefinition.createDefinition(
                    AgentType.createType("research"), "Research", "desc", "sys")));

    assertThat(agentService.listAgents("owner", "en")).hasSize(1);
    verify(pipelineService).listAgents("owner", "en");
  }

  @Test
  @DisplayName("should invoke sync and wrap AgentRun")
  void shouldInvokeSync() {
    when(pipelineService.invokeAgentSync("research", "hi", "owner", "en")).thenReturn("hello");

    AgentRun run = agentService.invoke("research", "hi", "owner", "en");

    assertThat(run.getReply()).isEqualTo("hello");
    assertThat(run.getAgentType()).isEqualTo("research");
  }

  @Test
  @DisplayName("should resolve language from Accept-Language")
  void shouldResolveLanguage() {
    assertThat(AgentService.resolveLanguage(null, "zh-CN,zh;q=0.9")).isEqualTo("zh");
    assertThat(AgentService.resolveLanguage("ja", "en")).isEqualTo("ja");
  }
}
