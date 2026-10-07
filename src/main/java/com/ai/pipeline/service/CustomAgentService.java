package com.ai.pipeline.service;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.CustomAgent;
import com.ai.pipeline.domain.model.CustomAgentId;
import com.ai.pipeline.domain.repository.CustomAgentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages a client's custom agent library, enforcing unique type keys per client. */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomAgentService {

  private final CustomAgentRepository repository;

  /** Lists the owner's custom agents. */
  public List<CustomAgent> listLibrary(String ownerKey) {
    return repository.findAllByOwnerKeyOrderByNameAsc(OwnerKey.parseKey(ownerKey));
  }

  /** Saves a new agent definition in the owner's library. */
  public CustomAgent create(
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    CustomAgent agent =
        CustomAgent.createAgent(ownerKey, typeKey, name, description, systemPrompt, toolKeys);
    assertTypeAvailable(ownerKey, agent.getTypeKey(), null);
    return repository.save(agent);
  }

  /** Updates the owner's custom agent. */
  public CustomAgent update(
      String ownerKey,
      String id,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    CustomAgent agent = findOwned(ownerKey, id);
    agent.update(name, description, systemPrompt, toolKeys);
    return repository.save(agent);
  }

  /** Enables or disables the owner's custom agent. */
  public CustomAgent setEnabled(String ownerKey, String id, boolean enabled) {
    CustomAgent agent = findOwned(ownerKey, id);
    agent.updateEnabledState(enabled);
    return repository.save(agent);
  }

  /** Deletes the owner's custom agent. */
  public void delete(String ownerKey, String id) {
    findOwned(ownerKey, id);
    repository.deleteByIdAndOwnerKey(CustomAgentId.parseId(id), OwnerKey.parseKey(ownerKey));
  }

  private CustomAgent findOwned(String ownerKey, String id) {
    return repository
        .findByIdAndOwnerKey(CustomAgentId.parseId(id), OwnerKey.parseKey(ownerKey))
        .orElseThrow(
            () ->
                DomainException.notFound("SAVED_AGENT_NOT_FOUND", "Custom agent not found: " + id));
  }

  private void assertTypeAvailable(String ownerKey, String typeKey, CustomAgentId excludeId) {
    if (repository.existsByOwnerKeyAndTypeKeyIgnoringId(
        OwnerKey.parseKey(ownerKey), typeKey, excludeId)) {
      throw DomainException.conflict(
          "SAVED_AGENT_TYPE_CONFLICT", "Agent type key already exists: " + typeKey);
    }
  }
}
