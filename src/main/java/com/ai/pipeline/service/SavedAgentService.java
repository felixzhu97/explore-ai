package com.ai.pipeline.service;

import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.pipeline.domain.model.SavedAgentId;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Manages a client's saved agent library, enforcing unique type keys per client. */
@Service
@RequiredArgsConstructor
public class SavedAgentService {

  private final SavedAgentRepository repository;

  /** Lists the owner's saved agents. */
  public List<SavedAgent> listLibrary(String ownerKey) {
    return repository.findAllByOwnerKey(ownerKey);
  }

  /** Saves a new agent definition in the owner's library. */
  public SavedAgent create(
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    SavedAgent agent =
        SavedAgent.create(ownerKey, typeKey, name, description, systemPrompt, toolKeys);
    assertTypeAvailable(ownerKey, agent.getTypeKey(), null);
    return repository.save(agent);
  }

  /** Updates the owner's saved agent. */
  public SavedAgent update(
      String ownerKey,
      String id,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    SavedAgent agent = findOwned(ownerKey, id);
    agent.update(name, description, systemPrompt, toolKeys);
    return repository.save(agent);
  }

  /** Enables or disables the owner's saved agent. */
  public SavedAgent setEnabled(String ownerKey, String id, boolean enabled) {
    SavedAgent agent = findOwned(ownerKey, id);
    agent.changeEnabled(enabled);
    return repository.save(agent);
  }

  /** Deletes the owner's saved agent. */
  public void delete(String ownerKey, String id) {
    findOwned(ownerKey, id);
    repository.deleteByIdAndOwnerKey(SavedAgentId.of(id), ownerKey);
  }

  private SavedAgent findOwned(String ownerKey, String id) {
    return repository
        .findByIdAndOwnerKey(SavedAgentId.of(id), ownerKey)
        .orElseThrow(
            () ->
                DomainException.notFound("SAVED_AGENT_NOT_FOUND", "Saved agent not found: " + id));
  }

  private void assertTypeAvailable(String ownerKey, String typeKey, SavedAgentId excludeId) {
    if (repository.existsByOwnerKeyAndTypeKeyIgnoringId(ownerKey, typeKey, excludeId)) {
      throw DomainException.conflict(
          "SAVED_AGENT_TYPE_CONFLICT", "Agent type key already exists: " + typeKey);
    }
  }
}
