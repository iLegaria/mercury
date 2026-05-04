package ianlegaria.personalknowledgeengine.snippet;

import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.common.exception.DuplicateSnippetException;
import ianlegaria.personalknowledgeengine.document.dto.DocumentResponse;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import ianlegaria.personalknowledgeengine.document.service.DocumentService;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardDeckRepository;
import ianlegaria.personalknowledgeengine.snippet.dto.CompileSnippetsRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.CreateSnippetRequest;
import ianlegaria.personalknowledgeengine.snippet.dto.SnippetResponse;
import ianlegaria.personalknowledgeengine.snippet.entity.SnippetEntity;
import ianlegaria.personalknowledgeengine.snippet.repository.SnippetRepository;
import ianlegaria.personalknowledgeengine.snippet.service.SnippetServiceImpl;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnippetServiceTest {

    @Mock SnippetRepository snippetRepository;
    @Mock UserRepository userRepository;
    @Mock DocumentService documentService;
    @Mock DocumentRepository documentRepository;
    @Mock ChunkRepository chunkRepository;
    @Mock EmbeddingService embeddingService;
    @Mock FlashcardDeckRepository deckRepository;
    @Mock FlashcardCardRepository cardRepository;
    @Mock CohereClient cohereClient;

    @InjectMocks SnippetServiceImpl snippetService;

    // ── createSnippet tests ──────────────────────────────────────────────────

    @Test
    void createSnippet_shortText_skipsJaccardCheckAndSaves() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("a@test.com").name("A").build();
        // fewer than 10 words → skip duplicate check
        String shortContent = "Short text here.";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(snippetRepository.save(any())).thenAnswer(inv -> {
            SnippetEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(OffsetDateTime.now());
            return s;
        });

        SnippetResponse result = snippetService.createSnippet(
                new CreateSnippetRequest(userId, shortContent, null, "Source"));

        verify(snippetRepository).save(any());
        assertThat(result.content()).isEqualTo(shortContent);
    }

    @Test
    void createSnippet_longTextIdenticalToExisting_throwsDuplicateSnippetException() {
        UUID userId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("b@test.com").name("B").build();
        // 10+ words, identical to existing → Jaccard = 1.0 ≥ 0.85
        String content = "The quick brown fox jumps over the lazy dog near the river";

        SnippetEntity existing = SnippetEntity.builder()
                .id(existingId).user(user).content(content).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(snippetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> snippetService.createSnippet(
                new CreateSnippetRequest(userId, content, null, null)))
                .isInstanceOf(DuplicateSnippetException.class);
    }

    @Test
    void createSnippet_longTextSufficientlyDifferent_saves() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("c@test.com").name("C").build();
        String newContent = "Machine learning models learn patterns from training data automatically and efficiently";
        String existingContent = "The stock market experienced volatility during financial crisis periods today";

        SnippetEntity existing = SnippetEntity.builder()
                .id(UUID.randomUUID()).user(user).content(existingContent).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(snippetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(existing));
        when(snippetRepository.save(any())).thenAnswer(inv -> {
            SnippetEntity s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(OffsetDateTime.now());
            return s;
        });

        snippetService.createSnippet(new CreateSnippetRequest(userId, newContent, null, null));

        verify(snippetRepository).save(any());
    }

    // ── compileSnippets tests ────────────────────────────────────────────────

    @Test
    void compileSnippets_formatsContentWithSourceAttribution() {
        UUID userId = UUID.randomUUID();
        UUID s1Id = UUID.randomUUID();
        UUID s2Id = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();

        SnippetEntity snippet1 = SnippetEntity.builder()
                .id(s1Id).user(user).content("First snippet content.")
                .sourceTitle("Article A").sourceUrl("https://a.com").build();
        SnippetEntity snippet2 = SnippetEntity.builder()
                .id(s2Id).user(user).content("Second snippet content.")
                .sourceTitle("Article B").sourceUrl("https://b.com").build();

        when(snippetRepository.findByIdInAndUserIdOrderByCreatedAtAsc(List.of(s1Id, s2Id), userId))
                .thenReturn(List.of(snippet1, snippet2));
        when(documentService.createFromText(eq(userId), anyString(), anyString(), isNull()))
                .thenReturn(DocumentResponse.builder().id(UUID.randomUUID()).title("Compiled").build());

        snippetService.compileSnippets(new CompileSnippetsRequest(userId, List.of(s1Id, s2Id), "Compiled"));

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentService).createFromText(eq(userId), eq("Compiled"), contentCaptor.capture(), isNull());

        String compiled = contentCaptor.getValue();
        assertThat(compiled).contains("First snippet content.");
        assertThat(compiled).contains("Second snippet content.");
        assertThat(compiled).contains("Article A");
        assertThat(compiled).contains("Article B");
        assertThat(compiled).contains("Snippet 1");
        assertThat(compiled).contains("Snippet 2");
    }
}
