package com.ai.analysis.infrastructure.llm;

import com.ai.analysis.domain.model.Sentiment;
import com.ai.analysis.domain.model.TextAnalysis;
import com.ai.analysis.domain.repository.StructuredAnalysisRepository;
import com.ai.analysis.domain.vo.AnalysisText;
import com.ai.analysis.domain.vo.LanguageHint;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.exception.AiServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SpringAiStructuredAnalysisRepository implements StructuredAnalysisRepository {

    private final ChatClientProvider chatClientProvider;

    public SpringAiStructuredAnalysisRepository(ChatClientProvider chatClientProvider) {
        this.chatClientProvider = chatClientProvider;
    }

    @Override
    public TextAnalysis analyze(AnalysisText text, LanguageHint hint) {
        LanguageHint effectiveHint = hint != null ? hint : LanguageHint.none();
        String prompt = text.buildAnalysisPrompt(effectiveHint);

        ChatClient chatClient = chatClientProvider.createStateless(TextChatOptions.withoutTools());
        StructuredAnalysisEntity entity = chatClient.prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(prompt)
                .call()
                .entity(StructuredAnalysisEntity.class);

        return toDomain(entity);
    }

    private static TextAnalysis toDomain(StructuredAnalysisEntity entity) {
        if (entity == null) {
            throw new AiServiceException("AI returned empty structured analysis response");
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
