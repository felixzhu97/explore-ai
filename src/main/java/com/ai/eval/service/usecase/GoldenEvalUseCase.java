package com.ai.eval.service.usecase;

import com.ai.chat.service.usecase.ChatUseCase;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.eval.domain.model.CaseEvalOutcome;
import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.model.GoldenSuiteReport;
import com.ai.eval.domain.model.OfficialGateResult;
import com.ai.eval.domain.repository.GoldenSuiteRepository;
import com.ai.eval.domain.vo.GoldenEvalDomain;
import com.ai.eval.infra.golden.GoldenRagFixtureSeeder;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.service.dto.RagChatResult;
import com.ai.rag.service.usecase.RagChatUseCase;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs OpenAI Evals-style golden cases against live Chat / RAG generation, then scores with Spring
 * AI official evaluators. Intended for tests (no REST).
 */
@Service
public class GoldenEvalUseCase {

  private static final Logger log = LoggerFactory.getLogger(GoldenEvalUseCase.class);
  private static final int ANSWER_LOG_LIMIT = 500;

  private final GoldenSuiteRepository suiteRepository;
  private final OfficialSpringAiEvaluators officialEvaluators;
  private final ChatUseCase chatUseCase;
  private final RagChatUseCase ragChatUseCase;
  private final GoldenRagFixtureSeeder fixtureSeeder;

  /** Documentation. */
  public GoldenEvalUseCase(
      GoldenSuiteRepository suiteRepository,
      OfficialSpringAiEvaluators officialEvaluators,
      ChatUseCase chatUseCase,
      RagChatUseCase ragChatUseCase,
      GoldenRagFixtureSeeder fixtureSeeder) {
    this.suiteRepository = suiteRepository;
    this.officialEvaluators = officialEvaluators;
    this.chatUseCase = chatUseCase;
    this.ragChatUseCase = ragChatUseCase;
    this.fixtureSeeder = fixtureSeeder;
  }

  /** Documentation. */
  public GoldenSuiteReport run(List<GoldenEvalDomain> domains, List<String> caseIds) {
    List<GoldenEvalCase> cases = suiteRepository.loadByDomains(domains);
    if (caseIds != null && !caseIds.isEmpty()) {
      Set<String> wanted = new LinkedHashSet<>(caseIds);
      cases = cases.stream().filter(c -> wanted.contains(c.id())).toList();
    }

    Map<String, String> fixtureIds =
        cases.stream().anyMatch(c -> c.domain() == GoldenEvalDomain.RAG)
            ? fixtureSeeder.ensureFixtures()
            : Map.of();

    List<CaseEvalOutcome> outcomes = new ArrayList<>();
    for (GoldenEvalCase evalCase : cases) {
      outcomes.add(runOne(evalCase, fixtureIds));
    }
    return GoldenSuiteReport.of(outcomes);
  }

  private CaseEvalOutcome runOne(GoldenEvalCase evalCase, Map<String, String> fixtureIds) {
    try {
      GeneratedAnswer generated = generate(evalCase, fixtureIds);
      List<String> context = resolveContext(evalCase, generated.contextTexts());
      OfficialGateResult gate =
          officialEvaluators.evaluate(evalCase.userText(), generated.answer(), context);
      return new CaseEvalOutcome(
          evalCase.id(),
          evalCase.domain(),
          evalCase.userText(),
          truncate(generated.answer()),
          gate.passed(),
          gate.relevancyPass(),
          gate.factualityPass(),
          gate.feedback(),
          null);
    } catch (RuntimeException ex) {
      log.warn("Golden case {} failed during generation/eval: {}", evalCase.id(), ex.toString());
      return new CaseEvalOutcome(
          evalCase.id(),
          evalCase.domain(),
          evalCase.userText(),
          "",
          false,
          false,
          null,
          List.of(),
          ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }
  }

  private GeneratedAnswer generate(GoldenEvalCase evalCase, Map<String, String> fixtureIds) {
    if (evalCase.domain() == GoldenEvalDomain.RAG) {
      List<String> docIds = resolveDocumentIds(evalCase, fixtureIds);
      RagChatResult result = ragChatUseCase.chat(evalCase.userText(), docIds, 5);
      List<String> sources =
          result.sources() == null
              ? List.of()
              : result.sources().stream()
                  .map(SourceDocument::text)
                  .filter(text -> text != null && !text.isBlank())
                  .toList();
      return new GeneratedAnswer(result.response() == null ? "" : result.response(), sources);
    }

    TextChatOptions options =
        evalCase.toolsEnabled() ? TextChatOptions.defaults() : TextChatOptions.withoutTools();
    String answer = chatUseCase.chat(evalCase.userText(), options);
    return new GeneratedAnswer(answer == null ? "" : answer, List.of());
  }

  private static List<String> resolveDocumentIds(
      GoldenEvalCase evalCase, Map<String, String> fixtureIds) {
    if (!evalCase.documentIds().isEmpty()) {
      return evalCase.documentIds();
    }
    List<String> ids = new ArrayList<>();
    for (String key : evalCase.fixtureKeys()) {
      String id = fixtureIds.get(key.toLowerCase(Locale.ROOT));
      if (id != null) {
        ids.add(id);
      }
    }
    return ids;
  }

  private static List<String> resolveContext(GoldenEvalCase evalCase, List<String> runtimeSources) {
    List<String> context = new ArrayList<>();
    if (runtimeSources != null) {
      context.addAll(runtimeSources);
    }
    if (context.isEmpty() && !evalCase.contexts().isEmpty()) {
      context.addAll(evalCase.contexts());
    }
    if (context.isEmpty()) {
      context.addAll(evalCase.ideal());
    }
    return List.copyOf(context);
  }

  private static String truncate(String answer) {
    if (answer.length() <= ANSWER_LOG_LIMIT) {
      return answer;
    }
    return answer.substring(0, ANSWER_LOG_LIMIT) + "…";
  }

  private record GeneratedAnswer(String answer, List<String> contextTexts) {}
}
