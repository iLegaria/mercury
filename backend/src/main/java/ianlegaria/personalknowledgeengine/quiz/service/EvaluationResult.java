package ianlegaria.personalknowledgeengine.quiz.service;

import java.util.Set;

public record EvaluationResult(String verdict, boolean isCorrect, String feedback) {

    private static final Set<String> VALID_VERDICTS = Set.of("CORRECT", "PARTIALLY_CORRECT", "INCORRECT");

    public static EvaluationResult parse(String verdictLine, String feedback) {
        String verdict = verdictLine.trim().toUpperCase();
        if (!VALID_VERDICTS.contains(verdict)) verdict = "INCORRECT";
        return new EvaluationResult(verdict, "CORRECT".equals(verdict), feedback);
    }
}
