package ianlegaria.personalknowledgeengine.quiz.event;

import java.util.UUID;

public record QuizAnswerIncorrectEvent(UUID userId, String question, String correctAnswer) {}
