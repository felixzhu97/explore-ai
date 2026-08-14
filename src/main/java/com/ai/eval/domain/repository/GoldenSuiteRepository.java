package com.ai.eval.domain.repository;

import com.ai.eval.domain.model.GoldenEvalCase;
import com.ai.eval.domain.vo.GoldenEvalDomain;
import java.util.List;

/** Loads OpenAI Evals JSONL golden cases from classpath. */
public interface GoldenSuiteRepository {
  /** Documentation. */
  List<GoldenEvalCase> loadAll();

  /** Documentation. */
  List<GoldenEvalCase> loadByDomains(List<GoldenEvalDomain> domains);
}
