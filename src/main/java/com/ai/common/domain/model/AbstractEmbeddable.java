package com.ai.common.domain.model;

import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Layer supertype of the value objects embedded in entities.
 *
 * <p>Typed ids are embedded ids, which Jakarta Persistence requires to be serializable, so value
 * objects inherit {@link Serializable} here instead of each declaring it.
 *
 * @see <a href="https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2">
 *     Jakarta Persistence 3.2 › Primary Keys and Entity Identity</a>
 */
@MappedSuperclass
public abstract class AbstractEmbeddable implements Serializable {}
