package ianlegaria.personalknowledgeengine.collection.controller;

import ianlegaria.personalknowledgeengine.collection.dto.CollectionResponse;
import ianlegaria.personalknowledgeengine.collection.dto.CreateCollectionRequest;
import ianlegaria.personalknowledgeengine.collection.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Collections", description = "Organize documents into named collections")
@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @Operation(summary = "Create collection")
    @ApiResponse(responseCode = "201", description = "Collection created")
    @PostMapping
    public ResponseEntity<CollectionResponse> createCollection(
            @Valid @RequestBody CreateCollectionRequest request) {
        CollectionResponse response = collectionService.createCollection(
                request.getUserId(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List user collections", description = "Returns collections for a user. Supports pagination via page/size/sort query params.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<CollectionResponse>> getCollectionsByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(collectionService.getCollectionsByUser(userId, pageable));
    }

    @Operation(summary = "Delete collection")
    @ApiResponse(responseCode = "204", description = "Collection deleted")
    @ApiResponse(responseCode = "404", description = "Collection not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable UUID id,
            @RequestParam UUID userId) {
        collectionService.deleteCollection(userId, id);
        return ResponseEntity.noContent().build();
    }
}
