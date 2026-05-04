package ianlegaria.personalknowledgeengine.document;

import ianlegaria.personalknowledgeengine.chunk.entity.ChunkEntity;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.document.dto.DocumentIngestionEvent;
import ianlegaria.personalknowledgeengine.document.service.IngestionConsumer;
import ianlegaria.personalknowledgeengine.document.service.IngestionPersistenceService;
import ianlegaria.personalknowledgeengine.document.service.TextExtractionService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionConsumerTest {

    @Mock TextExtractionService textExtractionService;
    @Mock EmbeddingService embeddingService;
    @Mock IngestionPersistenceService ingestionPersistenceService;

    private IngestionConsumer ingestionConsumer;

    @BeforeEach
    void setUp() {
        ingestionConsumer = new IngestionConsumer(
                textExtractionService, embeddingService, ingestionPersistenceService, new SimpleMeterRegistry());
    }

    @Test
    void processDocument_happyPath_callsAllThreePhasesInOrder() {
        UUID docId = UUID.randomUUID();
        ChunkEntity chunk = ChunkEntity.builder()
                .id(UUID.randomUUID()).content("some text").chunkIndex(0).build();

        when(ingestionPersistenceService.saveChunksWithoutEmbeddings(any(), anyString()))
                .thenReturn(List.of(chunk));
        when(embeddingService.generateDocumentEmbeddingsBatch(anyList()))
                .thenReturn(List.of(Collections.nCopies(1024, 0.1)));

        ingestionConsumer.processDocument(DocumentIngestionEvent.builder()
                .documentId(docId).title("Test").sourceType("TXT").textContent("some text here").build());

        InOrder inOrder = inOrder(ingestionPersistenceService, embeddingService);
        inOrder.verify(ingestionPersistenceService).saveChunksWithoutEmbeddings(any(), anyString());
        inOrder.verify(embeddingService).generateDocumentEmbeddingsBatch(anyList());
        inOrder.verify(ingestionPersistenceService).saveEmbeddingsAndComplete(any(), anyList(), anyList());
    }

    @Test
    void processDocument_emptyChunks_skipsEmbeddingAndCompletePhases() {
        UUID docId = UUID.randomUUID();
        when(ingestionPersistenceService.saveChunksWithoutEmbeddings(any(), any()))
                .thenReturn(Collections.emptyList());

        ingestionConsumer.processDocument(DocumentIngestionEvent.builder()
                .documentId(docId).title("Test").sourceType("TXT").textContent("").build());

        verify(embeddingService, never()).generateDocumentEmbeddingsBatch(anyList());
        verify(ingestionPersistenceService, never()).saveEmbeddingsAndComplete(any(), any(), any());
    }

    @Test
    void processDocument_embeddingFails_callsMarkFailed() {
        UUID docId = UUID.randomUUID();
        ChunkEntity chunk = ChunkEntity.builder()
                .id(UUID.randomUUID()).content("text").chunkIndex(0).build();

        when(ingestionPersistenceService.saveChunksWithoutEmbeddings(any(), any()))
                .thenReturn(List.of(chunk));
        when(embeddingService.generateDocumentEmbeddingsBatch(anyList()))
                .thenThrow(new RuntimeException("Cohere down"));

        assertThatThrownBy(() -> ingestionConsumer.processDocument(DocumentIngestionEvent.builder()
                .documentId(docId).title("Test").sourceType("TXT").textContent("some text").build()))
                .isInstanceOf(RuntimeException.class);

        verify(ingestionPersistenceService).markFailed(docId);
    }

    @Test
    void processDocument_extractionFails_callsMarkFailed() {
        UUID docId = UUID.randomUUID();
        when(textExtractionService.extractFromPath(any(Path.class)))
                .thenThrow(new RuntimeException("extraction failed"));

        assertThatThrownBy(() -> ingestionConsumer.processDocument(DocumentIngestionEvent.builder()
                .documentId(docId).title("Test").sourceType("TXT").filePath("/tmp/bad.txt").build()))
                .isInstanceOf(RuntimeException.class);

        verify(ingestionPersistenceService).markFailed(docId);
    }
}
