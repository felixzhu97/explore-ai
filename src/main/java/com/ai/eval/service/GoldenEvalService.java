package com.ai.eval.service;

import com.ai.chat.service.ChatService;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.eval.domain.model.CaseEvalOutcome;
import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.model.GoldenEvalCategory;
import com.ai.eval.domain.model.GoldenSuiteReport;
import com.ai.eval.domain.model.OfficialGateResult;
import com.ai.eval.domain.repository.GoldenSuiteRepository;
import com.ai.eval.infra.golden.GoldenRagFixtureSeeder;
import com.ai.rag.domain.model.SourceCitation;
import com.ai.rag.service.RagChatService;
import com.ai.rag.service.dto.RagChatResult;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Runs OpenAI Evals-style golden cases against live Chat / RAG generation, then scores with Spring
 * AI official evaluators. Intended for tests (no REST).
 */
@Service
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-eval",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
public class GoldenEvalService {

  private static final int ANSWER_EXCERPT_LIMIT = 500;

  private final GoldenSuiteRepository suiteRepository;
  private final OfficialSpringAiEvaluators officialEvaluators;
  private final ChatService chatService;
  private final RagChatService ragChatService;
  private final GoldenRagFixtureSeeder fixtureSeeder;

  /** Runs golden cases for the categories, optionally filtered by id, and aggregates a report. */
  public GoldenSuiteReport runSuite(List<GoldenEvalCategory> categories, List<String> caseIds) {
    List<GoldenEvalCase> cases = suiteRepository.loadByCategories(categories);
    if (caseIds != null && !caseIds.isEmpty()) {
      Set<String> wanted = new LinkedHashSet<>(caseIds);
      cases = cases.stream().filter(c -> wanted.contains(c.getId())).toList();
    }

    Map<String, String> fixtureIds =
        cases.stream().anyMatch(c -> c.getCategory() == GoldenEvalCategory.RAG)
            ? fixtureSeeder.ensureFixtures()
            : Map.of();

    List<CaseEvalOutcome> outcomes = new ArrayList<>();
    for (GoldenEvalCase evalCase : cases) {
      outcomes.add(runOne(evalCase, fixtureIds));
    }
    return GoldenSuiteReport.createReport(outcomes);
  }

  private CaseEvalOutcome runOne(GoldenEvalCase evalCase, Map<String, String> fixtureIds) {
    try {
      GeneratedAnswer generated = generateAnswer(evalCase, fixtureIds);
      List<String> context = resolveContext(evalCase, generated.contextTexts());
      OfficialGateResult gate =
          officialEvaluators.evaluateChat(evalCase.getUserText(), generated.answer(), context);
      return new CaseEvalOutcome(
          evalCase.getId(),
          evalCase.getCategory(),
          evalCase.getUserText(),
          truncateText(generated.answer()),
          gate.isPassed(),
          gate.isRelevancyPassed(),
          gate.getFactualityPassed(),
          gate.getFeedback(),
          null);
    } catch (RuntimeException ex) {
      return new CaseEvalOutcome(
          evalCase.getId(),
          evalCase.getCategory(),
          evalCase.getUserText(),
          "",
          false,
          false,
          null,
          List.of(),
          ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }
  }

  private GeneratedAnswer generateAnswer(GoldenEvalCase evalCase, Map<String, String> fixtureIds) {
    if (evalCase.getCategory() == GoldenEvalCategory.RAG) {
      List<String> documentIds = resolveDocumentIds(evalCase, fixtureIds);
      RagChatResult result =
          ragChatService.chatWithDocuments(
              evalCase.getUserText(), documentIds, 5, null, GoldenRagFixtureSeeder.OWNER_KEY);
      List<String> sources =
          result.sources().stream()
              .map(SourceCitation::getContent)
              .filter(text -> text != null && !text.isBlank())
              .toList();
      return new GeneratedAnswer(result.response(), sources);
    }

    TextChatOptions options =
        evalCase.isToolsEnabled()
            ? TextChatOptions.createDefaultOptions()
            : TextChatOptions.withoutTools();
    String answer = chatService.sendMessage(evalCase.getUserText(), options);
    return new GeneratedAnswer(answer == null ? "" : answer, List.of());
  }

  private static List<String> resolveDocumentIds(
      GoldenEvalCase evalCase, Map<String, String> fixtureIds) {
    if (!evalCase.getDocumentIds().isEmpty()) {
      return evalCase.getDocumentIds();
    }
    List<String> ids = new ArrayList<>();
    for (String key : evalCase.getFixtureKeys()) {
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
    if (context.isEmpty() && !evalCase.getContexts().isEmpty()) {
      context.addAll(evalCase.getContexts());
    }
    if (context.isEmpty()) {
      context.addAll(evalCase.getIdeal());
    }
    return List.copyOf(context);
  }

  private static String truncateText(String answer) {
    if (answer.length() <= ANSWER_EXCERPT_LIMIT) {
      return answer;
    }
    return answer.substring(0, ANSWER_EXCERPT_LIMIT) + "…";
  }

  private record GeneratedAnswer(String answer, List<String> contextTexts) {}
}
