package com.ai.a2a.domain.model;

/** Lifecycle state of an A2A Task. */
public enum A2aTaskStatus {
  SUBMITTED("submitted"),
  WORKING("working"),
  COMPLETED("completed"),
  FAILED("failed");

  private final String wireValue;

  A2aTaskStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  /** Returns the A2A wire value. */
  public String getWireValue() {
    return wireValue;
  }
}
