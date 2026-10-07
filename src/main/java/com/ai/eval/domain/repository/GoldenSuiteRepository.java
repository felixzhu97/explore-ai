package com.ai.eval.domain.repository;

import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.model.GoldenEvalCategory;
import java.util.List;

/** Loads OpenAI Evals JSONL golden cases from classpath. */
public interface GoldenSuiteRepository {
  /** Loads every golden case. */
  List<GoldenEvalCase> loadAll();

  /** Loads the golden cases of the categories. */
  List<GoldenEvalCase> loadByCategories(List<GoldenEvalCategory> categories);
}
