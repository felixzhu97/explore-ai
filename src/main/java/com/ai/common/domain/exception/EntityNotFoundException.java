package com.ai.common.domain.exception;

/** Raised when a domain entity cannot be found by id (optionally scoped to an owner). */
public class EntityNotFoundException extends AbstractDomainException {

  private final String resourceType;
  private final String resourceId;

  public EntityNotFoundException(String resourceType, String resourceId) {
    super(resourceType + " not found: " + resourceId);
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  /** Returns the kind of resource that was not found. */
  public String getResourceType() {
    return resourceType;
  }

  /** Returns the id that was not found. */
  public String getResourceId() {
    return resourceId;
  }
}
