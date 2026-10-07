package com.ai.eval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.service.ChatService;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.model.GoldenEvalCategory;
import com.ai.eval.domain.model.GoldenSuiteReport;
import com.ai.eval.domain.model.OfficialGateResult;
import com.ai.eval.domain.repository.GoldenSuiteRepository;
import com.ai.eval.infra.golden.GoldenRagFixtureSeeder;
import com.ai.rag.domain.model.SourceCitation;
import com.ai.rag.service.RagChatService;
import com.ai.rag.service.dto.RagChatResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoldenEvalService")
class GoldenEvalServiceTest {

  @Mock private GoldenSuiteRepository suiteRepository;
  @Mock private OfficialSpringAiEvaluators officialEvaluators;
  @Mock private ChatService chatService;
  @Mock private RagChatService ragChatService;
  @Mock private GoldenRagFixtureSeeder fixtureSeeder;

  @InjectMocks private GoldenEvalService useCase;

  @Test
  @DisplayName("should pass chat case when official gate passes")
  void shouldPassChatCaseWhenOfficialGatePasses() {
    GoldenEvalCase evalCase =
        new GoldenEvalCase(
            "chat-1",
            GoldenEvalCategory.CHAT,
            "What is Explore AI?",
            List.of("A demo platform"),
            false,
            List.of("Explore AI is a demo platform."),
            List.of(),
            List.of());
    when(suiteRepository.loadByCategories(List.of(GoldenEvalCategory.CHAT)))
        .thenReturn(List.of(evalCase));
    when(chatService.chat(eq("What is Explore AI?"), any(TextChatOptions.class)))
        .thenReturn("Explore AI is a demo platform.");
    when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
        .thenReturn(
            new OfficialGateResult(true, true, true, 1.0, 1.0, List.of("relevancy: PASS"), true));

    GoldenSuiteReport report = useCase.run(List.of(GoldenEvalCategory.CHAT), List.of());

    assertThat(report.getTotal()).isEqualTo(1);
    assertThat(report.getPassed()).isEqualTo(1);
    assertThat(report.getPassRate()).isEqualTo(1.0);
    assertThat(report.getCases().getFirst().isPassed()).isTrue();
    verify(fixtureSeeder, never()).ensureFixtures();
  }

  @Test
  @DisplayName("should use rag sources as context when rag case")
  void shouldUseRagSourcesAsContextWhenRagCase() {
    GoldenEvalCase evalCase =
        new GoldenEvalCase(
            "rag-1",
            GoldenEvalCategory.RAG,
            "What modules?",
            List.of("Chat", "RAG"),
            false,
            List.of(),
            List.of(),
            List.of("overview"));
    when(suiteRepository.loadByCategories(List.of(GoldenEvalCategory.RAG)))
        .thenReturn(List.of(evalCase));
    when(fixtureSeeder.ensureFixtures()).thenReturn(Map.of("overview", "doc-1"));
    when(ragChatService.chat(
            eq("What modules?"),
            eq(List.of("doc-1")),
            eq(5),
            isNull(),
            eq(GoldenRagFixtureSeeder.OWNER_KEY)))
        .thenReturn(
            new RagChatResult(
                "Chat and RAG",
                List.of(new SourceCitation("Core modules include Chat and RAG.", 0.9, Map.of()))));
    when(officialEvaluators.evaluate(eq("What modules?"), eq("Chat and RAG"), anyList()))
        .thenReturn(new OfficialGateResult(true, true, true, 1.0, 1.0, List.of(), true));

    GoldenSuiteReport report = useCase.run(List.of(GoldenEvalCategory.RAG), null);

    assertThat(report.getPassed()).isEqualTo(1);
    verify(ragChatService)
        .chat("What modules?", List.of("doc-1"), 5, null, GoldenRagFixtureSeeder.OWNER_KEY);
  }

  @Test
  @DisplayName("should mark failed when generation throws")
  void shouldMarkFailedWhenGenerationThrows() {
    GoldenEvalCase evalCase =
        new GoldenEvalCase(
            "chat-err",
            GoldenEvalCategory.CHAT,
            "boom",
            List.of("x"),
            false,
            List.of("x"),
            List.of(),
            List.of());
    when(suiteRepository.loadByCategories(any())).thenReturn(List.of(evalCase));
    when(chatService.chat(anyString(), any(TextChatOptions.class)))
        .thenThrow(new RuntimeException("provider down"));

    GoldenSuiteReport report = useCase.run(List.of(GoldenEvalCategory.CHAT), List.of("chat-err"));

    assertThat(report.getFailed()).isEqualTo(1);
    assertThat(report.getCases().getFirst().isPassed()).isFalse();
    assertThat(report.getCases().getFirst().getGenerationError()).contains("provider down");
    verify(officialEvaluators, never()).evaluate(anyString(), anyString(), anyList());
  }
}
