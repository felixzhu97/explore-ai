package com.ai.account.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for {@link ClientId} stored as linked_client_id column values. */
@Converter(autoApply = false)
public class ClientIdAttributeConverter implements AttributeConverter<ClientId, String> {

  @Override
  public String convertToDatabaseColumn(ClientId attribute) {
    return attribute == null ? null : attribute.getValue();
  }

  @Override
  public ClientId convertToEntityAttribute(String dbData) {
    return dbData == null || dbData.isBlank() ? null : ClientId.parseId(dbData);
  }
}
