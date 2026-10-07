package com.ai.textanalysis.infra.llm;

import com.ai.common.exception.DomainException;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.textanalysis.domain.model.AnalysisText;
import com.ai.textanalysis.domain.model.LanguageHint;
import com.ai.textanalysis.domain.model.Sentiment;
import com.ai.textanalysis.domain.model.TextAnalysis;
import com.ai.textanalysis.domain.repository.TextAnalysisGateway;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Repository;

/** Spring AI repository that asks the chat model for structured JSON text analysis. */
@Repository
@RequiredArgsConstructor
public class SpringAiTextAnalysisGateway implements TextAnalysisGateway {

  private final ChatClientProvider chatClientProvider;

  @Override
  public TextAnalysis analyzeText(AnalysisText text, LanguageHint hint) {
    LanguageHint effectiveHint = hint != null ? hint : LanguageHint.createEmptyHint();
    String prompt = text.buildAnalysisPrompt(effectiveHint);

    ChatClient chatClient = chatClientProvider.createStateless(TextChatOptions.withoutTools());
    StructuredAnalysisEntity entity =
        chatClient
            .prompt()
            .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
            .user(prompt)
            .call()
            .entity(StructuredAnalysisEntity.class);

    return toDomain(entity);
  }

  private static TextAnalysis toDomain(StructuredAnalysisEntity entity) {
    if (entity == null) {
      throw DomainException.createUnavailableError(
          "AI_SERVICE_ERROR", "AI returned empty structured analysis response");
    }
    return TextAnalysis.createAnalysis(
        entity.summary(),
        Sentiment.parseSentiment(entity.sentiment()),
        entity.keyPoints(),
        entity.entities(),
        entity.language());
  }

  record StructuredAnalysisEntity(
      String summary,
      String sentiment,
      List<String> keyPoints,
      List<String> entities,
      String language) {}
}
