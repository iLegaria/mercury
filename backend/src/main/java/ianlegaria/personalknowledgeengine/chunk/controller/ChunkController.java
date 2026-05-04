package ianlegaria.personalknowledgeengine.chunk.controller;

import ianlegaria.personalknowledgeengine.chunk.dto.ChunkResponse;
import ianlegaria.personalknowledgeengine.chunk.dto.UpdateChunkRequest;
import ianlegaria.personalknowledgeengine.chunk.service.ChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Chunks", description = "View and edit document chunks")
@RestController
@RequiredArgsConstructor
public class ChunkController {

    private final ChunkService chunkService;

    @Operation(summary = "List chunks for a document",
            description = "Returns all chunks ordered by chunk index. Use to reconstruct document content.")
    @GetMapping("/api/v1/documents/{documentId}/chunks")
    public ResponseEntity<List<ChunkResponse>> getChunks(
            @PathVariable UUID documentId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(chunkService.getChunksForDocument(documentId, userId));
    }

    @Operation(summary = "Update chunk content",
            description = "Updates the chunk text and immediately re-embeds it via Cohere.")
    @PatchMapping("/api/v1/chunks/{chunkId}")
    public ResponseEntity<ChunkResponse> updateChunk(
            @PathVariable UUID chunkId,
            @Valid @RequestBody UpdateChunkRequest req) {
        return ResponseEntity.ok(chunkService.updateChunk(chunkId, req.userId(), req.content()));
    }
}
