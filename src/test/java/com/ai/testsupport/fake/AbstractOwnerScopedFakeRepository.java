package com.ai.testsupport.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory CRUD helpers for client-id-owned named aggregates in unit tests.
 *
 * @param <E> entity type
 * @param <I> id type
 */
public abstract class AbstractOwnerScopedFakeRepository<E, I> {

  private final List<E> entities = new ArrayList<>();
  private int saveCount;

  /** Seeds an entity and returns it for test setup. */
  public E seed(E entity) {
    entities.add(entity);
    return entity;
  }

  /** Returns how many times {@link #save(Object)} was invoked. */
  public int saveCount() {
    return saveCount;
  }

  /** Returns the entity id. */
  protected abstract I getId(E entity);

  /** Returns the client id partition key. */
  protected abstract String getOwnerKeyValue(E entity);

  /** Returns the entity display name. */
  protected abstract String getName(E entity);

  /** Saves or replaces an entity by id. */
  public E save(E entity) {
    saveCount++;
    entities.removeIf(existing -> getId(existing).equals(getId(entity)));
    entities.add(entity);
    return entity;
  }

  /** Finds an entity by id scoped to client id. */
  public Optional<E> findByIdAndOwnerKey(I id, String ownerKey) {
    return entities.stream()
        .filter(entity -> getId(entity).equals(id) && getOwnerKeyValue(entity).equals(ownerKey))
        .findFirst();
  }

  /** Lists all entities for a client id. */
  public List<E> findAllByOwnerKey(String ownerKey) {
    return entities.stream().filter(entity -> getOwnerKeyValue(entity).equals(ownerKey)).toList();
  }

  /** Deletes an entity by id scoped to client id. */
  public void deleteByIdAndOwnerKey(I id, String ownerKey) {
    entities.removeIf(
        entity -> getId(entity).equals(id) && getOwnerKeyValue(entity).equals(ownerKey));
  }

  /** Returns whether another entity already uses the name for the client id. */
  public boolean existsByOwnerKeyAndNameIgnoringId(String ownerKey, String name, I excludeId) {
    return entities.stream()
        .filter(entity -> getOwnerKeyValue(entity).equals(ownerKey))
        .filter(entity -> getName(entity).equals(name))
        .anyMatch(entity -> excludeId == null || !getId(entity).equals(excludeId));
  }
}
