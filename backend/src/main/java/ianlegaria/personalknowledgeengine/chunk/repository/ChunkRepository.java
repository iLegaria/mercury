package ianlegaria.personalknowledgeengine.chunk.repository;

import ianlegaria.personalknowledgeengine.chunk.entity.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChunkRepository extends JpaRepository<ChunkEntity, UUID> {

    List<ChunkEntity> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    @Modifying
    @Query(value = "UPDATE chunks SET embedding = cast(:embedding as vector) WHERE id = :chunkId",
            nativeQuery = true)
    void updateEmbedding(@Param("chunkId") UUID chunkId, @Param("embedding") String embedding);

    @Query(value = """
        SELECT c.id, c.document_id, c.content, c.chunk_index,
               c.token_count, c.metadata, c.created_at,
               1 - (c.embedding <=> cast(:queryVector as vector)) as similarity,
               d.title as document_title
        FROM chunks c
        JOIN documents d ON c.document_id = d.id
        WHERE d.user_id = :userId
          AND c.embedding IS NOT NULL
        ORDER BY c.embedding <=> cast(:queryVector as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunks(
            @Param("userId") UUID userId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT c.id, c.document_id, c.content, c.chunk_index,
               c.token_count, c.metadata, c.created_at,
               1 - (c.embedding <=> cast(:queryVector as vector)) as similarity,
               d.title as document_title
        FROM chunks c
        JOIN documents d ON c.document_id = d.id
        WHERE d.user_id = :userId
          AND d.collection_id = :collectionId
          AND c.embedding IS NOT NULL
        ORDER BY c.embedding <=> cast(:queryVector as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarChunksInCollection(
            @Param("userId") UUID userId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit,
            @Param("collectionId") UUID collectionId
    );

    @Query(value = """
        SELECT c.id, c.content FROM chunks c
        JOIN documents d ON c.document_id = d.id
        WHERE d.user_id = :userId
          AND c.embedding IS NOT NULL
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRandomChunks(
            @Param("userId") UUID userId,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT c.id, c.content FROM chunks c
        JOIN documents d ON c.document_id = d.id
        WHERE d.user_id = :userId
          AND d.collection_id = :collectionId
          AND c.embedding IS NOT NULL
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRandomChunksInCollection(
            @Param("userId") UUID userId,
            @Param("collectionId") UUID collectionId,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT c.id, c.content FROM chunks c
        WHERE c.document_id = :documentId
          AND c.embedding IS NOT NULL
        ORDER BY c.chunk_index
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findChunksByDocumentId(
            @Param("documentId") UUID documentId,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT c.id, c.content FROM chunks c
        WHERE c.document_id = :documentId
          AND c.embedding IS NOT NULL
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRandomChunksByDocumentId(
            @Param("documentId") UUID documentId,
            @Param("limit") int limit
    );

    long countByDocumentId(UUID documentId);
}