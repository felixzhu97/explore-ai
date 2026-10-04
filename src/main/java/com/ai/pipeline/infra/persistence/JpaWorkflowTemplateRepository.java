package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.repository.WorkflowTemplateRepository;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA adapter for saved workflow templates. */
@Repository
public class JpaWorkflowTemplateRepository implements WorkflowTemplateRepository {

  private final SpringDataWorkflowTemplateRepository delegate;
  private static final Sort BY_NAME = Sort.by("name");

  private final OwnerPartitionScope ownerPartition;

  public JpaWorkflowTemplateRepository(
      SpringDataWorkflowTemplateRepository delegate, OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.ownerPartition = ownerPartition;
  }

  @Override
  @Transactional
  public SavedWorkflowTemplate save(SavedWorkflowTemplate template) {
    return delegate.saveAndFlush(template);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SavedWorkflowTemplate> findByIdAndOwnerKey(
      WorkflowTemplateId id, String ownerKey) {
    return ownerPartition.findOne(OwnerKey.parse(ownerKey), () -> delegate.findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedWorkflowTemplate> findAllByOwnerKey(String ownerKey) {
    return ownerPartition.apply(OwnerKey.parse(ownerKey), () -> delegate.findAll(BY_NAME));
  }

  @Override
  @Transactional
  public void deleteByIdAndOwnerKey(WorkflowTemplateId id, String ownerKey) {
    ownerPartition.run(OwnerKey.parse(ownerKey), () -> delegate.deleteById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByOwnerKeyAndNameIgnoringId(
      String ownerKey, String name, WorkflowTemplateId excludeId) {
    return ownerPartition.apply(
        OwnerKey.parse(ownerKey),
        () ->
            excludeId == null
                ? delegate.existsByName(name)
                : delegate.existsByNameAndIdNot(name, excludeId));
  }
}
