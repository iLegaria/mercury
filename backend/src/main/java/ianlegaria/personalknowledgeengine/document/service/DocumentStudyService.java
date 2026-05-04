package ianlegaria.personalknowledgeengine.document.service;

import ianlegaria.personalknowledgeengine.document.dto.DocumentStudyStatsResponse;
import ianlegaria.personalknowledgeengine.document.dto.GenerateFlashcardsRequest;
import ianlegaria.personalknowledgeengine.document.dto.StartDocumentQuizRequest;
import ianlegaria.personalknowledgeengine.flashcard.dto.DeckResponse;
import ianlegaria.personalknowledgeengine.quiz.dto.QuizSessionResponse;

import java.util.UUID;

public interface DocumentStudyService {

    DeckResponse generateFlashcards(UUID documentId, GenerateFlashcardsRequest req);

    QuizSessionResponse startDocumentQuiz(UUID documentId, StartDocumentQuizRequest req);

    DocumentStudyStatsResponse getStudyStats(UUID documentId, UUID userId);
}
