package ianlegaria.personalknowledgeengine.document.controller;

import ianlegaria.personalknowledgeengine.document.dto.*;
import ianlegaria.personalknowledgeengine.document.service.DocumentStudyService;
import ianlegaria.personalknowledgeengine.flashcard.dto.DeckResponse;
import ianlegaria.personalknowledgeengine.quiz.dto.QuizSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Document Study Hub", description = "Generate study materials from a specific document")
@RestController
@RequestMapping("/api/v1/documents/{documentId}/study")
@RequiredArgsConstructor
public class DocumentStudyController {

    private final DocumentStudyService documentStudyService;

    @Operation(summary = "Generate flashcards from document",
            description = """
                    Generates a flashcard deck from the document.
                    mode=EXTRACT: extracts existing Q&A pairs verbatim (ideal for study guides).
                    mode=GENERATE: LLM creates new flashcard pairs from the content.
                    Returns the existing deck if one was already generated for this document.
                    """)
    @PostMapping("/flashcards")
    public ResponseEntity<DeckResponse> generateFlashcards(
            @PathVariable UUID documentId,
            @Valid @RequestBody GenerateFlashcardsRequest req) {
        DeckResponse deck = documentStudyService.generateFlashcards(documentId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(deck);
    }

    @Operation(summary = "Start a quiz from document",
            description = """
                    Starts a quiz session scoped to this document.
                    generationMode=EXTRACT: uses existing Q&A pairs from the document as questions.
                    generationMode=GENERATE: LLM generates new questions from document chunks.
                    quizMode=STRICT|OPEN controls how answers are evaluated.
                    """)
    @PostMapping("/quiz")
    public ResponseEntity<QuizSessionResponse> startQuiz(
            @PathVariable UUID documentId,
            @Valid @RequestBody StartDocumentQuizRequest req) {
        QuizSessionResponse session = documentStudyService.startDocumentQuiz(documentId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @Operation(summary = "Get study stats for document",
            description = "Returns chunk count, quiz history, avg score, flashcard deck count and card count for this document.")
    @GetMapping("/stats")
    public DocumentStudyStatsResponse getStudyStats(
            @PathVariable UUID documentId,
            @RequestParam UUID userId) {
        return documentStudyService.getStudyStats(documentId, userId);
    }
}
