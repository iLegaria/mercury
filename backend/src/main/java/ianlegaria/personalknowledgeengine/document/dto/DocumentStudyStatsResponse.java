package ianlegaria.personalknowledgeengine.document.dto;

public record DocumentStudyStatsResponse(
        long chunkCount,
        long quizAttempts,
        long completedQuizzes,
        Double avgScore,
        long deckCount,
        long totalCards
) {}
