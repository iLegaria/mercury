package ianlegaria.personalknowledgeengine.snippet.service;

import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import ianlegaria.personalknowledgeengine.snippet.dto.AppendSnippetsRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.CompileSnippetsRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.CreateSnippetRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetActionResult;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetResponse;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetsToFlashcardsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SnippetService {

    SnippetResponse createSnippet(CreateSnippetRequest req);

    Page<SnippetResponse> getSnippetsByUser(UUID userId, Pageable pageable);

    void deleteSnippet(UUID snippetId, UUID userId);

    DocumentResponse compileSnippets(CompileSnippetsRequest req);

    SnippetActionResult appendToDocument(AppendSnippetsRequest req);

    SnippetActionResult createFlashcardsFromSnippets(SnippetsToFlashcardsRequest req);
}
