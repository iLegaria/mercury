package ianlegaria.personalknowledgeengine.chunk.service;

import ianlegaria.personalknowledgeengine.chunk.dto.ChunkResponse;
import ianlegaria.personalknowledgeengine.chunk.entity.ChunkEntity;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.document.entity.DocumentEntity;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkServiceImpl implements ChunkService {

    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public List<ChunkResponse> getChunksForDocument(UUID documentId, UUID userId) {
        DocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (!doc.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(c -> new ChunkResponse(c.getId(), c.getChunkIndex(), c.getContent(), c.getTokenCount()))
                .toList();
    }

    @Transactional
    public ChunkResponse updateChunk(UUID chunkId, UUID userId, String newContent) {
        ChunkEntity chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Chunk not found: " + chunkId));

        if (!chunk.getDocument().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        chunk.setContent(newContent);
        chunk.setTokenCount(newContent.split("\\s+").length);
        chunkRepository.save(chunk);

        List<Double> embedding = embeddingService.generateEmbedding(newContent);
        chunkRepository.updateEmbedding(chunkId, embeddingService.embeddingToString(embedding));

        log.info("Chunk {} updated and re-embedded", chunkId);
        return new ChunkResponse(chunk.getId(), chunk.getChunkIndex(), chunk.getContent(), chunk.getTokenCount());
    }
}
