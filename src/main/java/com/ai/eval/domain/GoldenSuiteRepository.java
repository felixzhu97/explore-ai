package com.ai.eval.domain;

import com.ai.eval.domain.GoldenEvalCase;
import com.ai.eval.domain.GoldenEvalDomain;

import java.util.List;

/**
 * Loads OpenAI Evals JSONL golden cases from classpath.
 */
public interface GoldenSuiteRepository {
    List<GoldenEvalCase> loadAll();

    List<GoldenEvalCase> loadByDomains(List<GoldenEvalDomain> domains);
}
