package com.ai.agent.domain.model;

import java.util.Locale;
import java.util.Objects;

public record AgentEvaluation(Verdict verdict, String feedback) {

    public enum Verdict {
        PASS,
        NEEDS_IMPROVEMENT,
        FAIL;

        public static Verdict parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return NEEDS_IMPROVEMENT;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "PASS" -> PASS;
                case "FAIL" -> FAIL;
                default -> NEEDS_IMPROVEMENT;
            };
        }
    }

    public AgentEvaluation {
        Objects.requireNonNull(verdict, "verdict");
        feedback = feedback == null ? "" : feedback;
    }

    public boolean passed() {
        return verdict == Verdict.PASS;
    }

    public boolean failed() {
        return verdict == Verdict.FAIL;
    }
}
