package com.ai.eval.application;

import com.ai.chat.application.ChatUseCase;
import com.ai.common.application.TextChatOptions;
import com.ai.eval.domain.GoldenEvalCase;
import com.ai.eval.domain.GoldenSuiteReport;
import com.ai.eval.domain.OfficialGateResult;
import com.ai.eval.domain.GoldenSuiteRepository;
import com.ai.eval.domain.GoldenEvalDomain;
import com.ai.eval.infrastructure.GoldenRagFixtureSeeder;
import com.ai.rag.application.RagChatResult;
import com.ai.rag.application.RagChatUseCase;
import com.ai.rag.domain.SourceDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoldenEvalUseCase")
class GoldenEvalUseCaseTest {

    @Mock
    private GoldenSuiteRepository suiteRepository;
    @Mock
    private OfficialSpringAiEvaluators officialEvaluators;
    @Mock
    private ChatUseCase chatUseCase;
    @Mock
    private RagChatUseCase ragChatUseCase;
    @Mock
    private GoldenRagFixtureSeeder fixtureSeeder;

    @InjectMocks
    private GoldenEvalUseCase useCase;

    @Test
    @DisplayName("should_passChatCase_when_officialGatePasses")
    void should_passChatCase_when_officialGatePasses() {
        GoldenEvalCase evalCase = new GoldenEvalCase(
                "chat-1",
                GoldenEvalDomain.CHAT,
                "What is Explore AI?",
                List.of("A demo platform"),
                false,
                List.of("Explore AI is a demo platform."),
                List.of(),
                List.of());
        when(suiteRepository.loadByDomains(List.of(GoldenEvalDomain.CHAT))).thenReturn(List.of(evalCase));
        when(chatUseCase.chat(eq("What is Explore AI?"), any(TextChatOptions.class)))
                .thenReturn("Explore AI is a demo platform.");
        when(officialEvaluators.evaluate(anyString(), anyString(), anyList()))
                .thenReturn(new OfficialGateResult(true, true, true, 1.0, 1.0, List.of("relevancy: PASS"), true));

        GoldenSuiteReport report = useCase.run(List.of(GoldenEvalDomain.CHAT), List.of());

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.passed()).isEqualTo(1);
        assertThat(report.passRate()).isEqualTo(1.0);
        assertThat(report.cases().getFirst().passed()).isTrue();
        verify(fixtureSeeder, never()).ensureFixtures();
    }

    @Test
    @DisplayName("should_useRagSourcesAsContext_when_ragCase")
    void should_useRagSourcesAsContext_when_ragCase() {
        GoldenEvalCase evalCase = new GoldenEvalCase(
                "rag-1",
                GoldenEvalDomain.RAG,
                "What modules?",
                List.of("Chat", "RAG"),
                false,
                List.of(),
                List.of(),
                List.of("overview"));
        when(suiteRepository.loadByDomains(List.of(GoldenEvalDomain.RAG))).thenReturn(List.of(evalCase));
        when(fixtureSeeder.ensureFixtures()).thenReturn(Map.of("overview", "doc-1"));
        when(ragChatUseCase.chat(eq("What modules?"), eq(List.of("doc-1")), eq(5)))
                .thenReturn(new RagChatResult(
                        "Chat and RAG",
                        List.of(new SourceDocument("Core modules include Chat and RAG.", 0.9, Map.of()))));
        when(officialEvaluators.evaluate(eq("What modules?"), eq("Chat and RAG"), anyList()))
                .thenReturn(new OfficialGateResult(true, true, true, 1.0, 1.0, List.of(), true));

        GoldenSuiteReport report = useCase.run(List.of(GoldenEvalDomain.RAG), null);

        assertThat(report.passed()).isEqualTo(1);
        verify(fixtureSeeder).ensureFixtures();
        verify(ragChatUseCase).chat("What modules?", List.of("doc-1"), 5);
    }

    @Test
    @DisplayName("should_markFailed_when_generationThrows")
    void should_markFailed_when_generationThrows() {
        GoldenEvalCase evalCase = new GoldenEvalCase(
                "chat-err",
                GoldenEvalDomain.CHAT,
                "boom",
                List.of("x"),
                false,
                List.of("x"),
                List.of(),
                List.of());
        when(suiteRepository.loadByDomains(any())).thenReturn(List.of(evalCase));
        when(chatUseCase.chat(anyString(), any(TextChatOptions.class)))
                .thenThrow(new RuntimeException("provider down"));

        GoldenSuiteReport report = useCase.run(List.of(GoldenEvalDomain.CHAT), List.of("chat-err"));

        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.cases().getFirst().passed()).isFalse();
        assertThat(report.cases().getFirst().generationError()).contains("provider down");
        verify(officialEvaluators, never()).evaluate(anyString(), anyString(), anyList());
    }
}
