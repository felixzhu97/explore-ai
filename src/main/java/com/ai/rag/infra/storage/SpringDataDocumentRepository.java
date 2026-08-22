package com.ai.rag.infra.storage;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.vo.DocumentId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for Document aggregate. */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<Document, DocumentId> {

  /** Documentation. */
  @Query(
      value =
          """
          SELECT * FROM documents
          WHERE owner_key = :ownerKeyValue
          ORDER BY created_at DESC
          """,
      nativeQuery = true)
  List<Document> findByOwnerKeyValueOrderByCreatedAtDesc(
      @Param("ownerKeyValue") String ownerKeyValue);

  /** Documentation. */
  @Query(
      value =
          """
          SELECT * FROM documents
          WHERE id = :id AND owner_key = :ownerKeyValue
          """,
      nativeQuery = true)
  Optional<Document> findByIdAndOwnerKeyValue(
      @Param("id") String id, @Param("ownerKeyValue") String ownerKeyValue);

  /** Documentation. */
  @Modifying
  @Query(
      value = "DELETE FROM documents WHERE id = :id AND owner_key = :ownerKeyValue",
      nativeQuery = true)
  void deleteByIdAndOwnerKeyValue(
      @Param("id") String id, @Param("ownerKeyValue") String ownerKeyValue);
}
