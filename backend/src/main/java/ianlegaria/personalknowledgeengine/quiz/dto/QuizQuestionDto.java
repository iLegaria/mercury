package ianlegaria.personalknowledgeengine.quiz.dto;

import java.util.UUID;

public record QuizQuestionDto(
        UUID questionId,
        String questionText,
        int questionIndex,
        int totalQuestions
) {}
