package com.ai.common.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OwnerKeysTest {

  @Test
  @DisplayName("should parse client id from client owner key")
  void shouldParseClientIdFromClientOwnerKey() {
    OwnerKey key = OwnerKeys.requireClient("abc");
    assertEquals("abc", OwnerKeys.parseClientId(key));
  }

  @Test
  @DisplayName("should reject account owner key when parsing client id")
  void shouldRejectAccountOwnerKeyWhenParsingClientId() {
    OwnerKey account = OwnerKey.forAccount("user-1");
    assertThrows(IllegalArgumentException.class, () -> OwnerKeys.parseClientId(account));
  }
}
