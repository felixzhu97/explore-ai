package com.ai.metrics.domain.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for {@link InvocationOutcome} stored as its lowercase wire value. */
@Converter(autoApply = false)
public class InvocationOutcomeAttributeConverter
    implements AttributeConverter<InvocationOutcome, String> {

  @Override
  public String convertToDatabaseColumn(InvocationOutcome attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public InvocationOutcome convertToEntityAttribute(String dbData) {
    return dbData == null || dbData.isBlank() ? null : InvocationOutcome.parse(dbData);
  }
}
