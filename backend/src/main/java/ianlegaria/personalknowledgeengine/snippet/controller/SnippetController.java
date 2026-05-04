package ianlegaria.personalknowledgeengine.snippet.controller;

import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import ianlegaria.personalknowledgeengine.snippet.dto.AppendSnippetsRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.CompileSnippetsRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.CreateSnippetRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetActionResult;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetResponse;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetsToFlashcardsRequest;
import ianlegaria.personalknowledgeengine.snippet.service.SnippetService;
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

@Tag(name = "Snippets", description = "Web snippet capture and RAG document compilation")
@RestController
@RequestMapping("/api/v1/snippets")
@RequiredArgsConstructor
public class SnippetController {

    private final SnippetService snippetService;

    @Operation(summary = "Save a snippet")
    @ApiResponse(responseCode = "201", description = "Snippet created")
    @PostMapping
    public ResponseEntity<SnippetResponse> createSnippet(@Valid @RequestBody CreateSnippetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(snippetService.createSnippet(request));
    }

    @Operation(summary = "List user snippets", description = "Returns snippets for a user. Supports pagination via page/size/sort query params.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<SnippetResponse>> getSnippetsByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(snippetService.getSnippetsByUser(userId, pageable));
    }

    @Operation(summary = "Delete a snippet")
    @ApiResponse(responseCode = "204", description = "Snippet deleted")
    @ApiResponse(responseCode = "404", description = "Snippet not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSnippet(@PathVariable UUID id, @RequestParam UUID userId) {
        snippetService.deleteSnippet(id, userId);
    }

    @Operation(summary = "Compile selected snippets into a document")
    @ApiResponse(responseCode = "200", description = "Document created and queued for ingestion")
    @PostMapping("/compile")
    public ResponseEntity<DocumentResponse> compileSnippets(@Valid @RequestBody CompileSnippetsRequest request) {
        return ResponseEntity.ok(snippetService.compileSnippets(request));
    }

    @Operation(summary = "Append snippets as chunks to an existing document")
    @ApiResponse(responseCode = "200", description = "Chunks appended and embedded")
    @PostMapping("/append")
    public ResponseEntity<SnippetActionResult> appendToDocument(@Valid @RequestBody AppendSnippetsRequest request) {
        return ResponseEntity.ok(snippetService.appendToDocument(request));
    }

    @Operation(summary = "Create flashcards from snippets in an existing deck")
    @ApiResponse(responseCode = "200", description = "Flashcards created")
    @PostMapping("/flashcards")
    public ResponseEntity<SnippetActionResult> createFlashcards(@Valid @RequestBody SnippetsToFlashcardsRequest request) {
        return ResponseEntity.ok(snippetService.createFlashcardsFromSnippets(request));
    }
}
