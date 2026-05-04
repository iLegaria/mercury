package ianlegaria.personalknowledgeengine.document.service;

import ianlegaria.personalknowledgeengine.chunk.entity.ChunkEntity;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.chunk.service.ChunkingService;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.common.config.ChunkingProperties;
import ianlegaria.personalknowledgeengine.common.config.RabbitMQConfig;
import ianlegaria.personalknowledgeengine.common.exception.DocumentNotPendingException;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.document.dto.DocumentIngestionEvent;
import ianlegaria.personalknowledgeengine.document.dto.FlashcardGenerationEvent;
import ianlegaria.personalknowledgeengine.document.entity.DocumentContentEntity;
import ianlegaria.personalknowledgeengine.document.entity.DocumentEntity;
import ianlegaria.personalknowledgeengine.document.repository.DocumentContentRepository;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionPersistenceServiceImpl implements IngestionPersistenceService {

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository documentContentRepository;
    private final ChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final RabbitTemplate rabbitTemplate;
    private final ChunkingService chunkingService;
    private final ChunkingProperties chunkingProperties;

    @Value("${flashcard.auto-generate.mode:GENERATE}")
    private String autoGenerateMode;

    @Value("${flashcard.auto-generate.num-cards:10}")
    private int autoGenerateNumCards;

    /**
     * Phase 1: persist document content and chunks without embeddings.
     * Short transaction — no external I/O, connection released immediately after.
     * Returns empty list if rawText is blank (document marked COMPLETED inline).
     */
    @Transactional
    public List<ChunkEntity> saveChunksWithoutEmbeddings(DocumentIngestionEvent event, String rawText) {
        DocumentEntity document = documentRepository.findById(event.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + event.getDocumentId()));

        if (!"PENDING".equals(document.getStatus())) {
            throw new DocumentNotPendingException(event.getDocumentId(), document.getStatus());
        }

        document.setStatus("PROCESSING");
        documentRepository.save(document);

        if (rawText == null || rawText.isBlank()) {
            log.warn("No text extracted from document: {}", event.getDocumentId());
            document.setStatus("COMPLETED");
            documentRepository.save(document);
            return Collections.emptyList();
        }

        DocumentContentEntity docContent = documentContentRepository.findByDocumentId(document.getId())
                .orElseGet(() -> DocumentContentEntity.builder().document(document).build());
        docContent.setContent(rawText);
        documentContentRepository.save(docContent);

        List<String> chunks = chunkingService.chunkText(rawText, chunkingProperties.getChunkSize(), chunkingProperties.getOverlap());
        chunkRepository.deleteByDocumentId(document.getId());
        return saveChunks(document, chunks);
    }

    /**
     * Phase 3: persist embeddings and mark document COMPLETED.
     * Short transaction — no external I/O.
     * Publishes flashcard generation event after commit.
     */
    @Transactional
    public void saveEmbeddingsAndComplete(DocumentIngestionEvent event, List<ChunkEntity> chunks, List<List<Double>> embeddings) {
        for (int i = 0; i < chunks.size(); i++) {
            String embeddingStr = embeddingService.embeddingToString(embeddings.get(i));
            chunkRepository.updateEmbedding(chunks.get(i).getId(), embeddingStr);
        }

        DocumentEntity document = documentRepository.findById(event.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + event.getDocumentId()));
        document.setStatus("COMPLETED");
        documentRepository.save(document);

        final UUID docId = event.getDocumentId();
        final UUID userId = event.getUserId();
        final String mode = autoGenerateMode;
        final int numCards = autoGenerateNumCards;

        Runnable publishEvent = () -> rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.FLASHCARD_ROUTING_KEY,
                FlashcardGenerationEvent.builder()
                        .documentId(docId)
                        .userId(userId)
                        .mode(mode)
                        .numCards(numCards)
                        .build()
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishEvent.run();
                }
            });
        } else {
            publishEvent.run();
        }
    }

    @Transactional
    public void markFailed(UUID documentId) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus("FAILED");
            documentRepository.save(doc);
        });
    }

    private List<ChunkEntity> saveChunks(DocumentEntity document, List<String> chunks) {
        List<ChunkEntity> entities = new ArrayList<>();
        int currentPos = 0;

        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            int startPos = currentPos;
            int endPos = startPos + content.length();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunkStrategy", "char_paragraph_aware");
            metadata.put("chunkSize", chunkingProperties.getChunkSize());
            metadata.put("overlap", chunkingProperties.getOverlap());
            metadata.put("startPosition", startPos);
            metadata.put("endPosition", endPos);

            entities.add(ChunkEntity.builder()
                    .document(document)
                    .content(content)
                    .chunkIndex(i)
                    .tokenCount(chunkingService.countTokens(content))
                    .metadata(metadata)
                    .build());

            currentPos = Math.max(0, endPos - chunkingProperties.getOverlap());
        }

        return chunkRepository.saveAll(entities);
    }
}
