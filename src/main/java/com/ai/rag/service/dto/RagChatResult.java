package com.ai.rag.service.dto;

import com.ai.rag.domain.model.SourceCitation;
import java.util.List;

public record RagChatResult(String response, List<SourceCitation> sources) {}
