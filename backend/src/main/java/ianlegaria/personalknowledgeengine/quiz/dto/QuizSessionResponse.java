package ianlegaria.personalknowledgeengine.quiz.dto;

import java.util.UUID;

public record QuizSessionResponse(
        UUID sessionId,
        int totalQuestions,
        int correctAnswers,
        String status,
        String mode,
        QuizQuestionDto currentQuestion  // null when status=COMPLETED
) {}
