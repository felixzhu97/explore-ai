package com.ai.metrics.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for {@link AiDomain} stored as its lowercase wire value. */
@Converter(autoApply = false)
public class AiDomainAttributeConverter implements AttributeConverter<AiDomain, String> {

  @Override
  public String convertToDatabaseColumn(AiDomain attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public AiDomain convertToEntityAttribute(String dbData) {
    return dbData == null || dbData.isBlank() ? null : AiDomain.require(dbData);
  }
}
