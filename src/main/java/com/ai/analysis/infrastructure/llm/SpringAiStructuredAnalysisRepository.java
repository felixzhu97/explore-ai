package com.ai.analysis.infrastructure.llm;

import com.ai.analysis.domain.model.Sentiment;
import com.ai.analysis.domain.model.TextAnalysis;
import com.ai.analysis.domain.repository.StructuredAnalysisRepository;
import com.ai.analysis.domain.vo.AnalysisText;
import com.ai.analysis.domain.vo.LanguageHint;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SpringAiStructuredAnalysisRepository implements StructuredAnalysisRepository {

    private final ChatClient chatClient;

    public SpringAiStructuredAnalysisRepository(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public TextAnalysis analyze(AnalysisText text, LanguageHint hint) {
        LanguageHint effectiveHint = hint != null ? hint : LanguageHint.none();
        String prompt = text.buildAnalysisPrompt(effectiveHint);

        StructuredAnalysisEntity entity = chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(prompt)
                .call()
                .entity(StructuredAnalysisEntity.class);

        return toDomain(entity);
    }

    private static TextAnalysis toDomain(StructuredAnalysisEntity entity) {
        if (entity == null) {
            return TextAnalysis.create(null, Sentiment.NEUTRAL, List.of(), List.of(), null);
        }
        return TextAnalysis.create(
                entity.summary(),
                Sentiment.fromString(entity.sentiment()),
                entity.keyPoints(),
                entity.entities(),
                entity.language());
    }

    record StructuredAnalysisEntity(
            @JsonProperty("summary") String summary,
            @JsonProperty("sentiment") String sentiment,
            @JsonProperty("key_points") List<String> keyPoints,
            @JsonProperty("entities") List<String> entities,
            @JsonProperty("language") String language) {
    }
}
