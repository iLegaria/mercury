package ianlegaria.personalknowledgeengine.quiz.dto;

public record AnswerFeedbackResponse(
        boolean isCorrect,
        String verdict,            // CORRECT, PARTIALLY_CORRECT, or INCORRECT
        String feedback,
        boolean sessionComplete,
        Integer finalScore,        // null while session is still active
        Integer totalQuestions,    // null while session is still active
        QuizQuestionDto nextQuestion  // null when sessionComplete=true
) {}
