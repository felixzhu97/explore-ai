package com.ai.rag.infrastructure.config;

import com.ai.rag.domain.repository.RagRetrievalSettings;
import org.springframework.stereotype.Component;

@Component
public class RagRetrievalSettingsAdapter implements RagRetrievalSettings {

    private final RagProperties ragProperties;

    public RagRetrievalSettingsAdapter(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    @Override
    public int getTopK() {
        return ragProperties.getRetrieval().getTopK();
    }

    @Override
    public double getScoreThreshold() {
        return ragProperties.getRetrieval().getScoreThreshold();
    }
}
