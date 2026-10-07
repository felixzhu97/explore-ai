package com.ai.account.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for {@link ContactEmail} stored as email column values. */
@Converter(autoApply = false)
public class ContactEmailAttributeConverter implements AttributeConverter<ContactEmail, String> {

  @Override
  public String convertToDatabaseColumn(ContactEmail attribute) {
    return attribute == null ? null : attribute.getValue();
  }

  @Override
  public ContactEmail convertToEntityAttribute(String dbData) {
    return ContactEmail.parseOptionalEmail(dbData);
  }
}
