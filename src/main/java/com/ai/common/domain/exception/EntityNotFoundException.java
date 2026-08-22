package com.ai.common.domain.exception;

/** Raised when a domain entity cannot be found by id (optionally scoped to an owner). */
public class EntityNotFoundException extends AbstractDomainException {

  private final String resourceType;
  private final String resourceId;

  /** Documentation. */
  public EntityNotFoundException(String resourceType, String resourceId) {
    super(resourceType + " not found: " + resourceId);
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  /** Documentation. */
  public String getResourceType() {
    return resourceType;
  }

  /** Documentation. */
  public String getResourceId() {
    return resourceId;
  }
}
