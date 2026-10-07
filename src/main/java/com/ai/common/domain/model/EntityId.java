package com.ai.common.domain.model;

/** Typed entity identifier backed by a UUID value. */
public interface EntityId {

  /** Returns the canonical string representation of this identifier. */
  String value();
}
