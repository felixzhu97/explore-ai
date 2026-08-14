package com.ai.pipeline.application.usecase;

import com.ai.pipeline.domain.exception.SavedAgentNotFoundException;
import com.ai.pipeline.domain.exception.SavedAgentTypeConflictException;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.repository.SavedAgentRepository;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import org.springframework.stereotype.Service;

/** Documentation. */
@Service
public class AgentDefinitionUseCaseImpl implements AgentDefinitionUseCase {

  private final SavedAgentRepository repository;

  /** Documentation. */
  public AgentDefinitionUseCaseImpl(SavedAgentRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SavedAgentDefinition> listLibrary(String clientId) {
    return repository.findAllByClientId(clientId);
  }

  @Override
  public SavedAgentDefinition create(
      String clientId,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    SavedAgentDefinition agent =
        SavedAgentDefinition.create(clientId, typeKey, name, description, systemPrompt, toolKeys);
    assertTypeAvailable(clientId, agent.getTypeKey(), null);
    return repository.save(agent);
  }

  @Override
  public SavedAgentDefinition update(
      String clientId,
      String id,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    SavedAgentDefinition agent = findOwned(clientId, id);
    agent.update(name, description, systemPrompt, toolKeys);
    return repository.save(agent);
  }

  @Override
  public SavedAgentDefinition setEnabled(String clientId, String id, boolean enabled) {
    SavedAgentDefinition agent = findOwned(clientId, id);
    if (enabled) {
      agent.enable();
    } else {
      agent.disable();
    }
    return repository.save(agent);
  }

  @Override
  public void delete(String clientId, String id) {
    findOwned(clientId, id);
    repository.deleteByIdAndClientId(SavedAgentId.of(id), clientId);
  }

  private SavedAgentDefinition findOwned(String clientId, String id) {
    return repository
        .findByIdAndClientId(SavedAgentId.of(id), clientId)
        .orElseThrow(() -> new SavedAgentNotFoundException(id));
  }

  private void assertTypeAvailable(String clientId, String typeKey, SavedAgentId excludeId) {
    if (repository.existsByClientIdAndTypeKeyIgnoringId(clientId, typeKey, excludeId)) {
      throw new SavedAgentTypeConflictException(typeKey);
    }
  }
}
