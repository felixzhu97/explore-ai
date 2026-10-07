package com.ai.pipeline.domain.repository;

import com.ai.common.domain.model.OwnerKey;
import com.ai.pipeline.domain.model.CustomAgent;
import com.ai.pipeline.domain.model.CustomAgentId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Persists custom agents scoped by owner, with type keys unique per owner. */
public interface CustomAgentRepository extends Repository<CustomAgent, CustomAgentId> {

  /** Finds the owner's custom agent by id. */
  Optional<CustomAgent> findByIdAndOwnerKey(CustomAgentId id, OwnerKey ownerKey);

  /** Lists the owner's custom agents by name. */
  List<CustomAgent> findAllByOwnerKeyOrderByNameAsc(OwnerKey ownerKey);

  /** Lists the owner's custom agents that are on, by name. */
  List<CustomAgent> findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(OwnerKey ownerKey);

  /** Tells whether the owner already has a custom agent with the type key. */
  boolean existsByOwnerKeyAndTypeKey(OwnerKey ownerKey, String typeKey);

  /** Tells whether another of the owner's custom agents already has the type key. */
  boolean existsByOwnerKeyAndTypeKeyAndIdNot(OwnerKey ownerKey, String typeKey, CustomAgentId id);

  /** Tells whether the owner has a custom agent with the type key other than {@code excludeId}. */
  default boolean existsByOwnerKeyAndTypeKeyIgnoringId(
      OwnerKey ownerKey, String typeKey, CustomAgentId excludeId) {
    return excludeId == null
        ? existsByOwnerKeyAndTypeKey(ownerKey, typeKey)
        : existsByOwnerKeyAndTypeKeyAndIdNot(ownerKey, typeKey, excludeId);
  }

  /** Saves the agent and returns the stored copy. */
  CustomAgent save(CustomAgent agent);

  /** Deletes the owner's custom agent by id. */
  @Transactional
  void deleteByIdAndOwnerKey(CustomAgentId id, OwnerKey ownerKey);
}
