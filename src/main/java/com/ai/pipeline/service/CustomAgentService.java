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
  public CustomAgent createAgent(
      String ownerKey,
      String agentType,
      String name,
      String description,
      String systemPrompt,
      List<String> tools) {
    CustomAgent agent =
        CustomAgent.createAgent(ownerKey, agentType, name, description, systemPrompt, tools);
    assertTypeAvailable(ownerKey, agent.getAgentType(), null);
    return repository.save(agent);
  }

  /** Updates the owner's custom agent. */
  public CustomAgent updateAgent(
      String ownerKey,
      String id,
      String name,
      String description,
      String systemPrompt,
      List<String> tools) {
    CustomAgent agent = findOwned(ownerKey, id);
    agent.update(name, description, systemPrompt, tools);
    return repository.save(agent);
  }

  /** Enables or disables the owner's custom agent. */
  public CustomAgent setEnabled(String ownerKey, String id, boolean enabled) {
    CustomAgent agent = findOwned(ownerKey, id);
    agent.updateEnabledState(enabled);
    return repository.save(agent);
  }

  /** Deletes the owner's custom agent. */
  public void deleteAgent(String ownerKey, String id) {
    findOwned(ownerKey, id);
    repository.deleteByIdAndOwnerKey(CustomAgentId.parseId(id), OwnerKey.parseKey(ownerKey));
  }

  private CustomAgent findOwned(String ownerKey, String id) {
    return repository
        .findByIdAndOwnerKey(CustomAgentId.parseId(id), OwnerKey.parseKey(ownerKey))
        .orElseThrow(
            () ->
                DomainException.createNotFoundError(
                    "SAVED_AGENT_NOT_FOUND", "Custom agent not found: " + id));
  }

  private void assertTypeAvailable(String ownerKey, String agentType, CustomAgentId excludeId) {
    if (repository.existsByOwnerKeyAndAgentTypeIgnoringId(
        OwnerKey.parseKey(ownerKey), agentType, excludeId)) {
      throw DomainException.createConflictError(
          "SAVED_AGENT_TYPE_CONFLICT", "Agent type key already exists: " + agentType);
    }
  }
}
