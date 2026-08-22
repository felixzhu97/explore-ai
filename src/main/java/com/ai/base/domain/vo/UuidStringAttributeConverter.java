package com.ai.base.domain.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

/** JPA converter between domain UUID strings and database UUID columns. */
@Converter(autoApply = false)
public class UuidStringAttributeConverter implements AttributeConverter<String, UUID> {

  @Override
  public UUID convertToDatabaseColumn(String attribute) {
    return attribute == null ? null : UUID.fromString(attribute);
  }

  @Override
  public String convertToEntityAttribute(UUID dbData) {
    return dbData == null ? null : dbData.toString();
  }
}
