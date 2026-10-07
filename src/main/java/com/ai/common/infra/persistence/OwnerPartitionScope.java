package com.ai.common.infra.persistence;

import com.ai.common.domain.model.AbstractOwnerKeyedEntity;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.domain.model.OwnerPartition;
import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs persistence work with the {@code ownerPartition} Hibernate filter enabled for one Owner Key,
 * so queries and loads by id only see that owner's rows.
 *
 * <p>Must be called inside a transaction: the filter lives on the transaction-bound session, and
 * the shared EntityManager opens a fresh session per call outside one.
 */
@Component
@RequiredArgsConstructor
public class OwnerPartitionScope {

  private final EntityManager entityManager;

  /** Returns the result of {@code work} with only {@code ownerKey} rows visible. */
  public <T> T apply(OwnerKey ownerKey, Supplier<T> work) {
    Objects.requireNonNull(ownerKey, "ownerKey");
    if (!TransactionSynchronizationManager.isActualTransactionActive()) {
      throw new IllegalStateException("Owner partition scope requires an active transaction");
    }
    Session session = entityManager.unwrap(Session.class);
    if (session.getEnabledFilter(OwnerPartition.FILTER_NAME) != null) {
      throw new IllegalStateException("Owner partition scope is already active");
    }
    session
        .enableFilter(OwnerPartition.FILTER_NAME)
        .setParameter(OwnerPartition.OWNER_KEY_PARAMETER, ownerKey.value());
    try {
      return work.get();
    } finally {
      session.disableFilter(OwnerPartition.FILTER_NAME);
    }
  }

  /**
   * Loads one aggregate inside the scope. The owner check also covers instances served from the
   * persistence context, which the filter never sees.
   */
  public <E extends AbstractOwnerKeyedEntity<?>> Optional<E> findOne(
      OwnerKey ownerKey, Supplier<Optional<E>> load) {
    return apply(ownerKey, load).filter(entity -> entity.belongsTo(ownerKey));
  }

  /** Runs {@code work} with only {@code ownerKey} rows visible. */
  public void run(OwnerKey ownerKey, Runnable work) {
    apply(
        ownerKey,
        () -> {
          work.run();
          return null;
        });
  }
}
