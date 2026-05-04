package ianlegaria.personalknowledgeengine.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.document.dto.*;
import ianlegaria.personalknowledgeengine.document.entity.DocumentEntity;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import ianlegaria.personalknowledgeengine.flashcard.dto.DeckResponse;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardDeckEntity;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardDeckRepository;
import ianlegaria.personalknowledgeengine.quiz.dto.QuizQuestionDto;
import ianlegaria.personalknowledgeengine.quiz.dto.QuizSessionResponse;
import ianlegaria.personalknowledgeengine.quiz.entity.QuizQuestionEntity;
import ianlegaria.personalknowledgeengine.quiz.entity.QuizSessionEntity;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizQuestionRepository;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizSessionRepository;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentStudyServiceImpl implements DocumentStudyService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ChunkRepository chunkRepository;
    private final FlashcardDeckRepository deckRepository;
    private final FlashcardCardRepository cardRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final CohereClient cohereClient;
    private final ObjectMapper objectMapper;

    private static final int MAX_EXTRACT_CHUNKS = 30;

    @Transactional
    public DeckResponse generateFlashcards(UUID documentId, GenerateFlashcardsRequest req) {
        DocumentEntity doc = verifyDocument(documentId, req.userId());

        List<FlashcardDeckEntity> existing = deckRepository.findBySourceDocumentId(documentId);
        if (!existing.isEmpty()) {
            FlashcardDeckEntity deck = existing.get(0);
            long cardCount = cardRepository.countByDeckId(deck.getId());
            long dueCount = cardRepository.countDueByDeckId(deck.getId(), OffsetDateTime.now());
            return toDeckResponse(deck, cardCount, dueCount);
        }

        String mode = req.resolvedMode();
        List<Object[]> chunks = "EXTRACT".equals(mode)
                ? chunkRepository.findChunksByDocumentId(documentId, MAX_EXTRACT_CHUNKS)
                : chunkRepository.findRandomChunksByDocumentId(documentId, req.resolvedNumCards());

        if (chunks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document has no indexed chunks yet");
        }

        List<FlashcardPair> pairs = "EXTRACT".equals(mode)
                ? extractQAPairs(chunks, req.resolvedNumCards())
                : generateQAPairs(chunks);

        if (pairs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not generate flashcards from this document");
        }

        UserEntity user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.userId()));

        String deckName = doc.getTitle();
        if (deckRepository.existsByUserIdAndName(req.userId(), deckName)) {
            deckName = deckName + " (" + java.time.LocalDate.now() + ")";
        }

        FlashcardDeckEntity deck = deckRepository.save(FlashcardDeckEntity.builder()
                .user(user)
                .name(deckName)
                .description("EXTRACT".equals(mode) ? "Extracted Q&A from document" : "Generated from document")
                .sourceDocumentId(documentId)
                .build());

        List<FlashcardCardEntity> cards = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            cards.add(FlashcardCardEntity.builder()
                    .deck(deck)
                    .question(pairs.get(i).question())
                    .answer(pairs.get(i).answer())
                    .cardIndex(i)
                    .build());
        }
        cardRepository.saveAll(cards);

        log.info("Generated {} flashcards ({} mode) for document {}", pairs.size(), mode, documentId);
        return toDeckResponse(deck, cards.size(), cards.size());
    }

    @Transactional
    public QuizSessionResponse startDocumentQuiz(UUID documentId, StartDocumentQuizRequest req) {
        DocumentEntity doc = verifyDocument(documentId, req.userId());

        String genMode = req.resolvedGenerationMode();
        String quizMode = "ADAPTIVE";

        List<Object[]> chunks = "EXTRACT".equals(genMode)
                ? chunkRepository.findChunksByDocumentId(documentId, MAX_EXTRACT_CHUNKS)
                : chunkRepository.findRandomChunksByDocumentId(documentId, req.resolvedNumQuestions());

        if (chunks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document has no indexed chunks yet");
        }

        List<String[]> generated = new ArrayList<>();
        if ("EXTRACT".equals(genMode)) {
            List<FlashcardPair> pairs = extractQAPairs(chunks, req.resolvedNumQuestions());
            for (FlashcardPair pair : pairs) {
                generated.add(new String[]{pair.question(), pair.answer()});
            }
        } else {
            for (Object[] chunk : chunks) {
                String content = (String) chunk[1];
                generated.add(new String[]{generateQuestion(content), content});
            }
        }

        if (generated.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not generate questions from this document");
        }

        UserEntity user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        QuizSessionEntity session = quizSessionRepository.save(
                QuizSessionEntity.builder()
                        .user(user)
                        .documentId(documentId)
                        .status("ACTIVE")
                        .mode(quizMode)
                        .totalQuestions(generated.size())
                        .correctAnswers(0)
                        .build()
        );

        List<QuizQuestionEntity> questions = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            questions.add(QuizQuestionEntity.builder()
                    .session(session)
                    .questionText(generated.get(i)[0])
                    .contextUsed(generated.get(i)[1])
                    .questionIndex(i)
                    .build());
        }
        quizQuestionRepository.saveAll(questions);

        log.info("Started document quiz ({} mode, {} questions) for document {}", genMode, generated.size(), documentId);

        QuizQuestionEntity first = questions.get(0);
        return new QuizSessionResponse(
                session.getId(),
                session.getTotalQuestions(),
                session.getCorrectAnswers(),
                session.getStatus(),
                session.getMode(),
                new QuizQuestionDto(first.getId(), first.getQuestionText(), 0, generated.size())
        );
    }

    @Transactional(readOnly = true)
    public DocumentStudyStatsResponse getStudyStats(UUID documentId, UUID userId) {
        long chunkCount = chunkRepository.countByDocumentId(documentId);

        List<QuizSessionEntity> sessions = quizSessionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
        long quizAttempts = sessions.size();
        List<QuizSessionEntity> completed = sessions.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus())).toList();
        Double avgScore = completed.isEmpty() ? null
                : completed.stream()
                        .mapToDouble(s -> s.getTotalQuestions() == 0 ? 0.0
                                : (double) s.getCorrectAnswers() / s.getTotalQuestions())
                        .average().orElse(0.0);

        List<FlashcardDeckEntity> decks = deckRepository.findBySourceDocumentId(documentId);
        long totalCards = decks.stream().mapToLong(d -> cardRepository.countByDeckId(d.getId())).sum();

        return new DocumentStudyStatsResponse(
                chunkCount, quizAttempts, completed.size(), avgScore, decks.size(), totalCards
        );
    }

    private DocumentEntity verifyDocument(UUID documentId, UUID userId) {
        DocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (!doc.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (!"COMPLETED".equals(doc.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is not fully processed yet");
        }
        return doc;
    }

    private List<FlashcardPair> extractQAPairs(List<Object[]> chunks, int maxCards) {
        StringBuilder sb = new StringBuilder();
        for (Object[] chunk : chunks) {
            sb.append((String) chunk[1]).append("\n\n");
        }
        String systemPrompt = """
                Extract all question-answer pairs from the text.
                Return ONLY a JSON array — no explanation, no markdown, no code fences.
                Format: [{"question": "...", "answer": "..."}, ...]
                If no Q&A pairs exist, return [].
                Extract verbatim — do not rephrase or summarize.""";
        String response = cohereClient.chat(systemPrompt, "Text:\n" + sb);
        return parseQAPairsJson(response, maxCards);
    }

    private List<FlashcardPair> generateQAPairs(List<Object[]> chunks) {
        List<FlashcardPair> pairs = new ArrayList<>();
        String systemPrompt = """
                Create one flashcard from the provided text.
                Return ONLY a JSON object — no explanation, no markdown, no code fences.
                Format: {"question": "...", "answer": "..."}""";
        for (Object[] chunk : chunks) {
            try {
                String response = cohereClient.chat(systemPrompt, "Text:\n" + chunk[1]);
                JsonNode node = objectMapper.readTree(cleanJson(response));
                String q = node.path("question").asText(null);
                String a = node.path("answer").asText(null);
                if (q != null && !q.isBlank() && a != null && !a.isBlank()) {
                    pairs.add(new FlashcardPair(q, a));
                }
            } catch (Exception e) {
                log.warn("Failed to parse flashcard: {}", e.getMessage());
            }
        }
        return pairs;
    }

    private List<FlashcardPair> parseQAPairsJson(String json, int maxCards) {
        try {
            String cleaned = cleanJson(json);
            int start = cleaned.indexOf('[');
            int end = cleaned.lastIndexOf(']') + 1;
            if (start < 0 || end <= start) return List.of();
            JsonNode arr = objectMapper.readTree(cleaned.substring(start, end));
            if (!arr.isArray()) return List.of();
            List<FlashcardPair> pairs = new ArrayList<>();
            for (JsonNode node : arr) {
                String q = node.path("question").asText(null);
                String a = node.path("answer").asText(null);
                if (q != null && !q.isBlank() && a != null && !a.isBlank()) {
                    pairs.add(new FlashcardPair(q, a));
                    if (pairs.size() >= maxCards) break;
                }
            }
            return pairs;
        } catch (Exception e) {
            log.warn("Failed to parse Q&A JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private String cleanJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("```(?:json)?", "").replaceAll("```\\s*$", "").trim();
        }
        return s;
    }

    private String generateQuestion(String chunkContent) {
        String systemPrompt = """
                You are a study quiz generator. Create ONE question based on the provided text \
                that tests genuine understanding — prefer questions that require the student to \
                explain, apply, or reason about a concept, not just recall a fact.
                Rules:
                1. The question must be answerable from the provided text.
                2. Return ONLY the question text. No preamble, no numbering, no explanation.""";
        return cohereClient.chat(systemPrompt, "Text:\n---\n" + chunkContent + "\n---");
    }

    private DeckResponse toDeckResponse(FlashcardDeckEntity deck, long cardCount, long dueCount) {
        return new DeckResponse(
                deck.getId(), deck.getUser().getId(), deck.getName(), deck.getDescription(),
                cardCount, dueCount, deck.getCreatedAt(), deck.isWhatsappReminderEnabled()
        );
    }

    private record FlashcardPair(String question, String answer) {}
}
