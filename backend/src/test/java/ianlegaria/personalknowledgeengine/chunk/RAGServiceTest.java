package ianlegaria.personalknowledgeengine.chunk;

import ianlegaria.personalknowledgeengine.chunk.dto.ChunkSearchResult;
import ianlegaria.personalknowledgeengine.chunk.dto.RAGResponse;
import ianlegaria.personalknowledgeengine.chunk.service.RAGServiceImpl;
import ianlegaria.personalknowledgeengine.chunk.service.SemanticSearchService;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.common.config.RagProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RAGServiceTest {

    @Mock SemanticSearchService semanticSearchService;
    @Mock CohereClient cohereClient;

    private RAGServiceImpl ragService;

    @BeforeEach
    void setUp() {
        RagProperties ragProperties = new RagProperties();
        ragService = new RAGServiceImpl(semanticSearchService, cohereClient, new SimpleMeterRegistry(), new ObjectMapper(), ragProperties);
    }

    @Test
    void ask_noChunksReturned_returnsNoInfoMessage() {
        UUID userId = UUID.randomUUID();
        when(semanticSearchService.search(any(), any(), anyInt(), isNull()))
                .thenReturn(List.of());

        RAGResponse response = ragService.ask(userId, "What is RAG?", null);

        assertThat(response.getAnswer()).contains("couldn't find");
        assertThat(response.getChunks()).isEmpty();
    }

    @Test
    void ask_allChunksBelowMinSimilarity_returnsNoInfoMessage() {
        UUID userId = UUID.randomUUID();

        ChunkSearchResult lowSimilarityChunk = ChunkSearchResult.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .content("some content")
                .chunkIndex(0)
                .similarity(0.1)  // below MIN_SIMILARITY of 0.3
                .build();

        when(semanticSearchService.search(any(), any(), anyInt(), isNull()))
                .thenReturn(List.of(lowSimilarityChunk));

        RAGResponse response = ragService.ask(userId, "What is RAG?", null);

        assertThat(response.getAnswer()).contains("couldn't find");
        assertThat(response.getChunks()).isEmpty();
    }

    @Test
    void ask_withValidChunk_callsCohereAndReturnsAnswer() {
        UUID userId = UUID.randomUUID();

        ChunkSearchResult relevantChunk = ChunkSearchResult.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .content("RAG combines retrieval with generation.")
                .chunkIndex(0)
                .similarity(0.8)
                .build();

        when(semanticSearchService.search(any(), any(), anyInt(), isNull()))
                .thenReturn(List.of(relevantChunk));
        when(cohereClient.chat(anyString(), anyString())).thenReturn("RAG stands for Retrieval-Augmented Generation.");

        RAGResponse response = ragService.ask(userId, "What is RAG?", null);

        assertThat(response.getAnswer()).isEqualTo("RAG stands for Retrieval-Augmented Generation.");
        assertThat(response.getChunks()).hasSize(1);
        assertThat(response.getChunks().get(0).getSimilarity()).isEqualTo(0.8);
    }

    @Test
    void ask_mixedSimilarityChunks_filtersOutLowSimilarity() {
        UUID userId = UUID.randomUUID();

        ChunkSearchResult high = ChunkSearchResult.builder()
                .chunkId(UUID.randomUUID()).documentId(UUID.randomUUID())
                .content("Relevant content about the topic.").chunkIndex(0).similarity(0.75).build();
        ChunkSearchResult low = ChunkSearchResult.builder()
                .chunkId(UUID.randomUUID()).documentId(UUID.randomUUID())
                .content("Barely related content.").chunkIndex(1).similarity(0.15).build();

        when(semanticSearchService.search(any(), any(), anyInt(), isNull()))
                .thenReturn(List.of(high, low));
        when(cohereClient.chat(anyString(), anyString())).thenReturn("Answer based on relevant content.");

        RAGResponse response = ragService.ask(userId, "Tell me about the topic", null);

        assertThat(response.getChunks()).hasSize(1);
        assertThat(response.getChunks().get(0).getSimilarity()).isEqualTo(0.75);
    }
}
