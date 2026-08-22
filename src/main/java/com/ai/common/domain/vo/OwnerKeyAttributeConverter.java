package com.ai.common.domain.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for {@link OwnerKey} stored as owner_key column values. */
@Converter(autoApply = false)
public class OwnerKeyAttributeConverter implements AttributeConverter<OwnerKey, String> {

  @Override
  public String convertToDatabaseColumn(OwnerKey attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public OwnerKey convertToEntityAttribute(String dbData) {
    return dbData == null || dbData.isBlank() ? null : OwnerKey.parse(dbData);
  }
}
