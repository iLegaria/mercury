package ianlegaria.personalknowledgeengine.document.service;

import ianlegaria.personalknowledgeengine.chunk.entity.ChunkEntity;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.common.config.RabbitMQConfig;
import ianlegaria.personalknowledgeengine.common.exception.DocumentNotPendingException;
import ianlegaria.personalknowledgeengine.document.dto.DocumentIngestionEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionConsumer {

    private final TextExtractionService textExtractionService;
    private final EmbeddingService embeddingService;
    private final IngestionPersistenceService ingestionPersistenceService;
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = RabbitMQConfig.INGESTION_QUEUE)
    public void processDocument(DocumentIngestionEvent event) {
        log.info("Processing document: {} ({})", event.getTitle(), event.getDocumentId());

        try {
            // Phase 1: persist text and chunks — short TX, no external I/O
            String rawText = extractText(event);
            List<ChunkEntity> savedChunks = ingestionPersistenceService.saveChunksWithoutEmbeddings(event, rawText);

            if (savedChunks.isEmpty()) {
                meterRegistry.counter("documents.processed", "status", "completed").increment();
                return;
            }

            // Phase 2: generate embeddings — outside TX, Cohere HTTP calls
            List<List<Double>> embeddings = generateEmbeddings(savedChunks);

            // Phase 3: persist embeddings and mark COMPLETED — short TX
            ingestionPersistenceService.saveEmbeddingsAndComplete(event, savedChunks, embeddings);

            meterRegistry.counter("documents.processed", "status", "completed").increment();
            log.info("Document processed successfully: {} — {} chunks embedded",
                    event.getDocumentId(), savedChunks.size());

        } catch (DocumentNotPendingException e) {
            log.warn("Skipping document {}: {}", event.getDocumentId(), e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic lock conflict on document {}, another consumer already claimed it — skipping",
                    event.getDocumentId());
        } catch (Exception e) {
            log.error("Failed to process document: {}", event.getDocumentId(), e);
            meterRegistry.counter("documents.processed", "status", "failed").increment();
            ingestionPersistenceService.markFailed(event.getDocumentId());
        }
    }

    private String extractText(DocumentIngestionEvent event) {
        if (event.getTextContent() != null && !event.getTextContent().isBlank()) {
            return event.getTextContent();
        }
        if (event.getFilePath() != null && !event.getFilePath().isBlank()) {
            Path path = Paths.get(event.getFilePath());
            return textExtractionService.extractFromPath(path);
        }
        return "Document: " + event.getTitle();
    }

    private List<List<Double>> generateEmbeddings(List<ChunkEntity> chunks) {
        List<String> texts = chunks.stream().map(ChunkEntity::getContent).toList();
        return embeddingService.generateDocumentEmbeddingsBatch(texts);
    }
}
