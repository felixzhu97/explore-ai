package com.ai.base.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidStringAttributeConverterTest {

  private final UuidStringAttributeConverter converter = new UuidStringAttributeConverter();

  @Test
  @DisplayName("should round-trip uuid string through database column")
  void shouldRoundTripUuidStringThroughDatabaseColumn() {
    String uuid = UUID.randomUUID().toString();
    UUID column = converter.convertToDatabaseColumn(uuid);
    assertEquals(uuid, converter.convertToEntityAttribute(column));
  }

  @Test
  @DisplayName("should map null attribute to null column")
  void shouldMapNullAttributeToNullColumn() {
    assertNull(converter.convertToDatabaseColumn(null));
    assertNull(converter.convertToEntityAttribute(null));
  }
}
