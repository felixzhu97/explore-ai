package com.ai.common.infra.persistence;

/** Names of the Hibernate filter that scopes owner-keyed aggregates to one Owner Key. */
public final class OwnerPartition {

  public static final String FILTER_NAME = "ownerPartition";
  public static final String OWNER_KEY_PARAMETER = "ownerKey";

  private OwnerPartition() {}
}
