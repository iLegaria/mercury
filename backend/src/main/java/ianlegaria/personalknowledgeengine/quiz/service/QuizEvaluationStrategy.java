package ianlegaria.personalknowledgeengine.quiz.service;

public interface QuizEvaluationStrategy {

    EvaluationResult evaluate(String question, String userAnswer, String context);

    String systemPrompt();

    QuizMode supportedMode();
}
