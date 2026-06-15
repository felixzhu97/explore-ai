package com.ai.infrastructure.event;

import com.ai.interfaces.dto.SourceDocumentDto;

import java.util.List;

public record SourcesEvent(List<SourceDocumentDto> sources) {}
