package com.ai.common.domain.repository;

import com.ai.base.domain.vo.AbstractUuidId;
import java.util.List;
import java.util.Optional;

/** Shared contract for client-id-partitioned aggregates with unique names per client. */
public interface ClientIdOwnedNamedRepository<E, I extends AbstractUuidId> {

  /** Documentation. */
  E save(E entity);

  /** Documentation. */
  Optional<E> findByIdAndClientId(I id, String clientId);

  /** Documentation. */
  List<E> findAllByClientId(String clientId);

  /** Documentation. */
  void deleteByIdAndClientId(I id, String clientId);

  /** Documentation. */
  boolean existsByClientIdAndNameIgnoringId(String clientId, String name, I excludeId);
}
