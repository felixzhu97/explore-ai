package com.ai.common.domain.repository;

import com.ai.base.domain.vo.AbstractUuidId;
import java.util.List;
import java.util.Optional;

/** Shared contract for client-id-partitioned aggregates with unique names per client. */
public interface ClientIdOwnedNamedRepository<E, I extends AbstractUuidId> {

  E save(E entity);

  Optional<E> findByIdAndClientId(I id, String clientId);

  List<E> findAllByClientId(String clientId);

  void deleteByIdAndClientId(I id, String clientId);

  boolean existsByClientIdAndNameIgnoringId(String clientId, String name, I excludeId);
}
