package com.ai.common.domain.exception;

/** Raised when a create or rename would duplicate a unique name within an owner partition. */
public class NameConflictException extends AbstractDomainException {

  private final String resourceType;
  private final String name;

  /** Documentation. */
  public NameConflictException(String resourceType, String name) {
    super(resourceType + " name already exists: " + name);
    this.resourceType = resourceType;
    this.name = name;
  }

  /** Documentation. */
  public String getResourceType() {
    return resourceType;
  }

  /** Documentation. */
  public String getName() {
    return name;
  }
}
