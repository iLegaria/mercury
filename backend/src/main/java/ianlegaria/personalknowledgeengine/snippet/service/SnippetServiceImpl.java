package ianlegaria.personalknowledgeengine.snippet.service;

import ianlegaria.personalknowledgeengine.chunk.entity.ChunkEntity;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.common.exception.DuplicateSnippetException;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import ianlegaria.personalknowledgeengine.document.entity.DocumentEntity;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import ianlegaria.personalknowledgeengine.document.service.DocumentService;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardDeckEntity;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardDeckRepository;
import ianlegaria.personalknowledgeengine.snippet.dto.AppendSnippetsRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.CompileSnippetsRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.CreateSnippetRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetActionResult;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetResponse;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetsToFlashcardsRequest;
import ianlegaria.personalknowledgeengine.snippet.entity.SnippetEntity;
import ianlegaria.personalknowledgeengine.snippet.repository.SnippetRepository;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnippetServiceImpl implements SnippetService {

    private final SnippetRepository snippetRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final FlashcardDeckRepository deckRepository;
    private final FlashcardCardRepository cardRepository;
    private final CohereClient cohereClient;

    private static final String QUESTION_PROMPT = """
            You are a study quiz generator. Create ONE question based on the provided text \
            that tests genuine understanding — prefer questions that require the student to \
            explain, apply, or reason about a concept, not just recall a fact.
            Rules:
            1. The question must be answerable from the provided text.
            2. Return ONLY the question text. No preamble, no numbering, no explanation.""";

    @Transactional
    public SnippetResponse createSnippet(CreateSnippetRequest req) {
        UserEntity user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + req.userId()));

        if (req.content().split("\\W+").length >= 10) {
            snippetRepository.findByUserIdOrderByCreatedAtDesc(req.userId()).stream()
                    .filter(s -> jaccardSimilarity(req.content(), s.getContent()) >= 0.85)
                    .findFirst()
                    .ifPresent(s -> { throw new DuplicateSnippetException(s.getId()); });
        }

        SnippetEntity saved = snippetRepository.save(SnippetEntity.builder()
                .user(user)
                .content(req.content())
                .sourceUrl(req.sourceUrl())
                .sourceTitle(req.sourceTitle())
                .build());

        log.info("Created snippet for user {}: {} chars from '{}'", req.userId(), req.content().length(), req.sourceTitle());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SnippetResponse> getSnippetsByUser(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return snippetRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public void deleteSnippet(UUID snippetId, UUID userId) {
        SnippetEntity snippet = snippetRepository.findByIdAndUserId(snippetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Snippet not found with id: " + snippetId));
        snippetRepository.delete(snippet);
        log.info("Deleted snippet {} for user {}", snippetId, userId);
    }

    @Transactional
    public DocumentResponse compileSnippets(CompileSnippetsRequest req) {
        List<SnippetEntity> snippets = snippetRepository
                .findByIdInAndUserIdOrderByCreatedAtAsc(req.snippetIds(), req.userId());

        if (snippets.isEmpty()) {
            throw new ResourceNotFoundException("No valid snippets found for compilation");
        }

        AtomicInteger counter = new AtomicInteger(1);
        String compiledContent = snippets.stream()
                .map(s -> {
                    String title = s.getSourceTitle() != null ? s.getSourceTitle() : "Unknown";
                    String url = s.getSourceUrl() != null ? s.getSourceUrl() : "N/A";
                    return String.format("--- Snippet %d ---%nSource: %s (%s)%n%n%s%n",
                            counter.getAndIncrement(), title, url, s.getContent());
                })
                .reduce("", (a, b) -> a + b);

        log.info("Compiling {} snippets into document '{}' for user {}", snippets.size(), req.documentTitle(), req.userId());
        return documentService.createFromText(req.userId(), req.documentTitle(), compiledContent, null);
    }

    @Transactional
    public SnippetActionResult appendToDocument(AppendSnippetsRequest req) {
        DocumentEntity doc = documentRepository.findById(req.documentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + req.documentId()));
        if (!doc.getUser().getId().equals(req.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (!"COMPLETED".equals(doc.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is not fully processed yet");
        }

        List<SnippetEntity> snippets = snippetRepository
                .findByIdInAndUserIdOrderByCreatedAtAsc(req.snippetIds(), req.userId());
        if (snippets.isEmpty()) {
            throw new ResourceNotFoundException("No valid snippets found");
        }

        int nextIndex = (int) chunkRepository.countByDocumentId(req.documentId());
        for (SnippetEntity snippet : snippets) {
            ChunkEntity chunk = chunkRepository.save(ChunkEntity.builder()
                    .document(doc)
                    .content(snippet.getContent())
                    .chunkIndex(nextIndex++)
                    .build());
            List<Double> emb = embeddingService.generateEmbedding(snippet.getContent());
            chunkRepository.updateEmbedding(chunk.getId(), embeddingService.embeddingToString(emb));
        }

        log.info("Appended {} snippet(s) as chunks to document {} for user {}", snippets.size(), req.documentId(), req.userId());
        return new SnippetActionResult(snippets.size());
    }

    @Transactional
    public SnippetActionResult createFlashcardsFromSnippets(SnippetsToFlashcardsRequest req) {
        FlashcardDeckEntity deck = deckRepository.findByIdAndUserId(req.deckId(), req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + req.deckId()));

        List<SnippetEntity> snippets = snippetRepository
                .findByIdInAndUserIdOrderByCreatedAtAsc(req.snippetIds(), req.userId());
        if (snippets.isEmpty()) {
            throw new ResourceNotFoundException("No valid snippets found");
        }

        int nextIndex = (int) cardRepository.countByDeckId(req.deckId());
        for (SnippetEntity snippet : snippets) {
            String question = cohereClient.chat(QUESTION_PROMPT, "Text:\n---\n" + snippet.getContent() + "\n---");
            cardRepository.save(FlashcardCardEntity.builder()
                    .deck(deck)
                    .question(question)
                    .answer(snippet.getContent())
                    .cardIndex(nextIndex++)
                    .build());
        }

        log.info("Created {} flashcard(s) from snippets in deck {} for user {}", snippets.size(), req.deckId(), req.userId());
        return new SnippetActionResult(snippets.size());
    }

    private double jaccardSimilarity(String a, String b) {
        Set<String> wa = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\W+")));
        Set<String> wb = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\W+")));
        long intersection = wa.stream().filter(wb::contains).count();
        long union = wa.size() + wb.size() - intersection;
        return union == 0 ? 0 : (double) intersection / union;
    }

    private SnippetResponse toResponse(SnippetEntity entity) {
        return new SnippetResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getContent(),
                entity.getSourceUrl(),
                entity.getSourceTitle(),
                entity.getCreatedAt()
        );
    }
}
