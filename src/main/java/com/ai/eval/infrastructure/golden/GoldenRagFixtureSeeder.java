package com.ai.eval.infrastructure.golden;

import com.ai.rag.application.usecase.DocumentUploadService;
import com.ai.rag.domain.model.Document;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Idempotent seeder for RAG golden fixtures under {@code classpath:eval/golden/fixtures/*}.
 * Resolves logical fixture keys (filename without extension) to uploaded document UUIDs.
 */
@Component
public class GoldenRagFixtureSeeder {

  private static final Logger log = LoggerFactory.getLogger(GoldenRagFixtureSeeder.class);
  private static final String PATTERN = "classpath:eval/golden/fixtures/*";
  private static final String TITLE_PREFIX = "golden-fixture-";

  private final DocumentUploadService documentUploadService;
  private final PathMatchingResourcePatternResolver resolver =
      new PathMatchingResourcePatternResolver();

  /** Documentation. */
  public GoldenRagFixtureSeeder(DocumentUploadService documentUploadService) {
    this.documentUploadService = documentUploadService;
  }

  /**
   * Ensures golden RAG fixture documents exist and returns their ids.
   *
   * @return map of fixture key → document UUID string
   */
  public Map<String, String> ensureFixtures() {
    try {
      Map<String, String> byTitle = indexExistingByTitle();
      Map<String, String> resolved = new HashMap<>();
      Resource[] resources = resolver.getResources(PATTERN);
      for (Resource resource : resources) {
        String filename = resource.getFilename();
        if (filename == null || filename.startsWith(".")) {
          continue;
        }
        String key = stripExtension(filename).toLowerCase(Locale.ROOT);
        String title = TITLE_PREFIX + key;
        String existingId = byTitle.get(title);
        if (existingId != null) {
          resolved.put(key, existingId);
          continue;
        }
        String content = resource.getContentAsString(StandardCharsets.UTF_8);
        DocumentUploadService.UploadResult uploaded =
            documentUploadService.upload(
                title,
                filename,
                (long) content.getBytes(StandardCharsets.UTF_8).length,
                content,
                "c:eval-golden");
        String id = uploaded.documentId().value().toString();
        resolved.put(key, id);
        byTitle.put(title, id);
        log.info("Seeded golden RAG fixture {} as document {}", key, id);
      }
      return Map.copyOf(resolved);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to seed golden RAG fixtures", ex);
    }
  }

  private Map<String, String> indexExistingByTitle() {
    List<Document> documents = documentUploadService.listAll("c:eval-golden");
    Map<String, String> byTitle = new HashMap<>();
    for (Document document : documents) {
      if (document.getTitle() != null && document.getTitle().startsWith(TITLE_PREFIX)) {
        byTitle.put(document.getTitle(), document.getId().value().toString());
      }
    }
    return byTitle;
  }

  private static String stripExtension(String filename) {
    int dot = filename.lastIndexOf('.');
    return dot > 0 ? filename.substring(0, dot) : filename;
  }
}
