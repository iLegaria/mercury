package ianlegaria.personalknowledgeengine.document.controller;

import ianlegaria.personalknowledgeengine.document.dto.AssignCollectionRequest;
import ianlegaria.personalknowledgeengine.document.dto.CreateDocumentRequest;
import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import ianlegaria.personalknowledgeengine.document.dto.UpdateDocumentContentRequest;
import ianlegaria.personalknowledgeengine.document.dto.UpdateDocumentTitleRequest;
import ianlegaria.personalknowledgeengine.document.service.DocumentService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Tag(name = "Documents", description = "Document management and async ingestion pipeline")
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Upload and ingest document",
            description = """
                    Uploads a file (PDF, DOCX, TXT, HTML) and enqueues it for async processing via RabbitMQ.
                    The document status transitions: PENDING → PROCESSING → COMPLETED (or FAILED).
                    Text is extracted with Apache Tika, chunked into ~500-word segments with 50-word overlap,
                    embedded with Cohere embed-multilingual-v3.0, and stored in pgvector for semantic search.
                    """)
    @ApiResponse(responseCode = "201", description = "Document uploaded and queued for ingestion")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("userId") UUID userId,
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "collectionId", required = false) UUID collectionId) {
        DocumentResponse response = documentService.uploadDocument(userId, title, file, collectionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Create document from pasted text",
            description = "Creates a document from raw text content (no file upload). Text is chunked and embedded directly.")
    @ApiResponse(responseCode = "201", description = "Document created and queued for ingestion")
    @PostMapping("/create-text")
    public ResponseEntity<DocumentResponse> createFromText(@Valid @RequestBody CreateDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createFromText(request.userId(), request.title(), request.content(), request.collectionId()));
    }

    @Operation(summary = "List user documents",
            description = "Returns documents for a user, optionally filtered by collection. Supports pagination via page/size/sort query params.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsByUser(
            @PathVariable UUID userId,
            @RequestParam(value = "collectionId", required = false) UUID collectionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(documentService.getDocumentsByUser(userId, Optional.ofNullable(collectionId), pageable));
    }

    @Operation(summary = "Get document by ID")
    @ApiResponse(responseCode = "200", description = "Document found")
    @ApiResponse(responseCode = "404", description = "Document not found")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        DocumentResponse response = documentService.getDocumentById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete document",
            description = "Deletes the document record and its associated file from disk.")
    @ApiResponse(responseCode = "204", description = "Document deleted")
    @ApiResponse(responseCode = "404", description = "Document not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id, @RequestParam UUID userId) {
        documentService.deleteDocument(id, userId);
    }

    @Operation(summary = "Update document content",
            description = "Replaces document text, deletes existing chunks, and re-queues for ingestion.")
    @ApiResponse(responseCode = "200", description = "Content updated and re-queued")
    @PatchMapping("/{id}/content")
    public ResponseEntity<DocumentResponse> updateContent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentContentRequest request) {
        return ResponseEntity.ok(documentService.updateContent(id, request.userId(), request.content()));
    }

    @Operation(summary = "Update document title")
    @ApiResponse(responseCode = "200", description = "Title updated")
    @ApiResponse(responseCode = "404", description = "Document not found")
    @PatchMapping("/{id}/title")
    public ResponseEntity<DocumentResponse> updateTitle(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentTitleRequest request) {
        return ResponseEntity.ok(documentService.updateTitle(id, request.userId(), request.title()));
    }

    @Operation(summary = "Assign document to collection")
    @ApiResponse(responseCode = "200", description = "Collection assigned")
    @ApiResponse(responseCode = "404", description = "Document or collection not found")
    @PatchMapping("/{id}/collection")
    public ResponseEntity<DocumentResponse> assignCollection(
            @PathVariable UUID id,
            @Valid @RequestBody AssignCollectionRequest request) {
        DocumentResponse response = documentService.assignCollection(id, request.getUserId(), request.getCollectionId());
        return ResponseEntity.ok(response);
    }
}
