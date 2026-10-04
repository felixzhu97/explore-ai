package com.ai.pipeline.service;

import com.ai.pipeline.domain.exception.SavedAgentNotFoundException;
import com.ai.pipeline.domain.exception.SavedAgentTypeConflictException;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import org.springframework.stereotype.Service;

/** Manages a client's saved agent library, enforcing unique type keys per client. */
@Service
public class SavedAgentService {

  private final SavedAgentRepository repository;

  public SavedAgentService(SavedAgentRepository repository) {
    this.repository = repository;
  }

  public List<SavedAgentDefinition> listLibrary(String ownerKey) {
    return repository.findAllByOwnerKey(ownerKey);
  }

  /** Saves a new agent definition in the owner's library. */
  public SavedAgentDefinition create(
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    SavedAgentDefinition agent =
        SavedAgentDefinition.create(ownerKey, typeKey, name, description, systemPrompt, toolKeys);
    assertTypeAvailable(ownerKey, agent.getTypeKey(), null);
    return repository.save(agent);
  }

  public SavedAgentDefinition update(
      String ownerKey,
      String id,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    SavedAgentDefinition agent = findOwned(ownerKey, id);
    agent.update(name, description, systemPrompt, toolKeys);
    return repository.save(agent);
  }

  /** Enables or disables the owner's saved agent. */
  public SavedAgentDefinition setEnabled(String ownerKey, String id, boolean enabled) {
    SavedAgentDefinition agent = findOwned(ownerKey, id);
    if (enabled) {
      agent.enable();
    } else {
      agent.disable();
    }
    return repository.save(agent);
  }

  public void delete(String ownerKey, String id) {
    findOwned(ownerKey, id);
    repository.deleteByIdAndOwnerKey(SavedAgentId.of(id), ownerKey);
  }

  private SavedAgentDefinition findOwned(String ownerKey, String id) {
    return repository
        .findByIdAndOwnerKey(SavedAgentId.of(id), ownerKey)
        .orElseThrow(() -> new SavedAgentNotFoundException(id));
  }

  private void assertTypeAvailable(String ownerKey, String typeKey, SavedAgentId excludeId) {
    if (repository.existsByOwnerKeyAndTypeKeyIgnoringId(ownerKey, typeKey, excludeId)) {
      throw new SavedAgentTypeConflictException(typeKey);
    }
  }
}
