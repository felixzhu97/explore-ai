package com.ai.common.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores every {@link OwnerKey} attribute as its owner_key column value. */
@Converter(autoApply = true)
public class OwnerKeyAttributeConverter implements AttributeConverter<OwnerKey, String> {

  @Override
  public String convertToDatabaseColumn(OwnerKey attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public OwnerKey convertToEntityAttribute(String dbData) {
    return dbData == null || dbData.isBlank() ? null : OwnerKey.parseKey(dbData);
  }
}
