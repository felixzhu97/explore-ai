package com.ai.automation.domain.model;

import com.ai.common.domain.model.AbstractEmbeddable;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/** Strongly-typed ID for {@link com.ai.automation.domain.model.AutomationRun}. */
@Embeddable
@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(staticName = "createId")
public final class RunId extends AbstractEmbeddable {

  @NonNull private UUID value;

  /** Parses an id from its UUID text. */
  public static RunId parseId(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Id cannot be blank");
    }
    return createId(UUID.fromString(text.strip()));
  }

  /** Creates a new random id. */
  public static RunId generateId() {
    return createId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
