package com.ai.base.domain.vo;

/** Typed entity identifier backed by a String UUID value. */
public interface EntityId {

  /** Returns the canonical string representation of this identifier. */
  String value();
}
