package ianlegaria.personalknowledgeengine.document.service;

import ianlegaria.personalknowledgeengine.common.config.RabbitMQConfig;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.document.dto.FlashcardGenerationEvent;
import ianlegaria.personalknowledgeengine.document.dto.GenerateFlashcardsRequest;
import ianlegaria.personalknowledgeengine.flashcard.dto.DeckResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardGenerationConsumer {

    private final DocumentStudyService documentStudyService;

    @RabbitListener(queues = RabbitMQConfig.FLASHCARD_QUEUE)
    public void generateFlashcards(FlashcardGenerationEvent event) {
        log.info("Auto-generating flashcards for document: {}", event.getDocumentId());
        try {
            GenerateFlashcardsRequest req = new GenerateFlashcardsRequest(
                    event.getUserId(), event.getMode(), event.getNumCards());
            DeckResponse deck = documentStudyService.generateFlashcards(event.getDocumentId(), req);
            log.info("Auto-generated deck '{}' ({} cards) for document {}",
                    deck.name(), deck.cardCount(), event.getDocumentId());
        } catch (ResourceNotFoundException e) {
            // Permanent: document/user deleted between ingestion and this message.
            log.warn("Flashcard auto-gen skipped — resource gone for document {}: {}",
                    event.getDocumentId(), e.getMessage());
        } catch (ResponseStatusException e) {
            // Permanent: FORBIDDEN (wrong userId) or BAD_REQUEST (no chunks / not COMPLETED).
            log.warn("Flashcard auto-gen skipped for document {}: {} {}",
                    event.getDocumentId(), e.getStatusCode(), e.getReason());
        } catch (Exception e) {
            // Transient: Cohere down, circuit breaker open, network error.
            // Rethrow → Spring AMQP nacks → TTL 30s → DLQ.
            log.error("Flashcard auto-gen failed (will retry) for document {}: {}",
                    event.getDocumentId(), e.getMessage(), e);
            throw e;
        }
    }
}
