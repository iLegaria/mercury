package ianlegaria.personalknowledgeengine.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.document.dto.DocumentStudyStatsResponse;
import ianlegaria.personalknowledgeengine.document.dto.GenerateFlashcardsRequest;
import ianlegaria.personalknowledgeengine.document.entity.DocumentEntity;
import ianlegaria.personalknowledgeengine.document.repository.DocumentRepository;
import ianlegaria.personalknowledgeengine.document.service.DocumentStudyServiceImpl;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardDeckEntity;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardDeckRepository;
import ianlegaria.personalknowledgeengine.quiz.entity.QuizSessionEntity;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizQuestionRepository;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizSessionRepository;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentStudyServiceTest {

    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock ChunkRepository chunkRepository;
    @Mock FlashcardDeckRepository deckRepository;
    @Mock FlashcardCardRepository cardRepository;
    @Mock QuizSessionRepository quizSessionRepository;
    @Mock QuizQuestionRepository quizQuestionRepository;
    @Mock CohereClient cohereClient;

    private DocumentStudyServiceImpl studyService;

    @BeforeEach
    void setUp() {
        studyService = new DocumentStudyServiceImpl(
                documentRepository, userRepository, chunkRepository,
                deckRepository, cardRepository, quizSessionRepository,
                quizQuestionRepository, cohereClient, new ObjectMapper());
    }

    @Test
    void generateFlashcards_documentNotCompleted_throwsBadRequest() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("a@test.com").name("A").build();
        DocumentEntity doc = DocumentEntity.builder().id(documentId).user(user).status("PENDING").title("Doc").build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> studyService.generateFlashcards(documentId, new GenerateFlashcardsRequest(userId, "GENERATE", 5)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateFlashcards_extractMode_parsesJsonAndCreatesCards() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("b@test.com").name("B").build();
        DocumentEntity doc = DocumentEntity.builder().id(documentId).user(user).status("COMPLETED").title("Study Guide").build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(deckRepository.findBySourceDocumentId(documentId)).thenReturn(List.of());
        when(chunkRepository.findChunksByDocumentId(any(), anyInt())).thenReturn(chunk("Some chunk text about topic A."));
        when(cohereClient.chat(anyString(), anyString())).thenReturn(
                "[{\"question\":\"What is topic A?\",\"answer\":\"Topic A is the main concept.\"}]");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deckRepository.existsByUserIdAndName(userId, "Study Guide")).thenReturn(false);
        FlashcardDeckEntity savedDeck = FlashcardDeckEntity.builder().id(deckId).user(user).name("Study Guide").build();
        when(deckRepository.save(any())).thenReturn(savedDeck);
        when(cardRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        studyService.generateFlashcards(documentId, new GenerateFlashcardsRequest(userId, "EXTRACT", 10));

        ArgumentCaptor<List<FlashcardCardEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(cardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getQuestion()).isEqualTo("What is topic A?");
    }

    @Test
    void generateFlashcards_malformedLlmResponse_throwsBadRequest() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("c@test.com").name("C").build();
        DocumentEntity doc = DocumentEntity.builder().id(documentId).user(user).status("COMPLETED").title("Doc").build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(deckRepository.findBySourceDocumentId(documentId)).thenReturn(List.of());
        when(chunkRepository.findChunksByDocumentId(any(), anyInt())).thenReturn(chunk("Some chunk text."));
        when(cohereClient.chat(anyString(), anyString())).thenReturn("not valid json at all");

        assertThatThrownBy(() -> studyService.generateFlashcards(documentId, new GenerateFlashcardsRequest(userId, "EXTRACT", 10)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void generateFlashcards_duplicateDeckName_appendsDateSuffix() {
        UUID documentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("d@test.com").name("D").build();
        DocumentEntity doc = DocumentEntity.builder().id(documentId).user(user).status("COMPLETED").title("My Doc").build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
        when(deckRepository.findBySourceDocumentId(documentId)).thenReturn(List.of());
        when(chunkRepository.findChunksByDocumentId(any(), anyInt())).thenReturn(chunk("Chunk content here."));
        when(cohereClient.chat(anyString(), anyString())).thenReturn("[{\"question\":\"Q?\",\"answer\":\"A.\"}]");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deckRepository.existsByUserIdAndName(userId, "My Doc")).thenReturn(true);
        FlashcardDeckEntity savedDeck = FlashcardDeckEntity.builder().id(deckId).user(user).name("My Doc (2026-05-04)").build();
        when(deckRepository.save(any())).thenReturn(savedDeck);
        when(cardRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        studyService.generateFlashcards(documentId, new GenerateFlashcardsRequest(userId, "EXTRACT", 10));

        ArgumentCaptor<FlashcardDeckEntity> captor = ArgumentCaptor.forClass(FlashcardDeckEntity.class);
        verify(deckRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).startsWith("My Doc (");
    }

    @Test
    void getStudyStats_aggregatesAllFields() {
        UUID documentId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();

        when(chunkRepository.countByDocumentId(documentId)).thenReturn(5L);

        QuizSessionEntity completed = QuizSessionEntity.builder()
                .status("COMPLETED").totalQuestions(10).correctAnswers(8).build();
        QuizSessionEntity active = QuizSessionEntity.builder()
                .status("ACTIVE").totalQuestions(10).correctAnswers(0).build();
        when(quizSessionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId))
                .thenReturn(List.of(completed, active));

        FlashcardDeckEntity deck = FlashcardDeckEntity.builder().id(deckId).build();
        when(deckRepository.findBySourceDocumentId(documentId)).thenReturn(List.of(deck));
        when(cardRepository.countByDeckId(deckId)).thenReturn(12L);

        DocumentStudyStatsResponse stats = studyService.getStudyStats(documentId, UUID.randomUUID());

        assertThat(stats.chunkCount()).isEqualTo(5);
        assertThat(stats.quizAttempts()).isEqualTo(2);
        assertThat(stats.completedQuizzes()).isEqualTo(1);
        assertThat(stats.avgScore()).isNotNull();
        assertThat(stats.avgScore()).isEqualTo(0.8);
        assertThat(stats.deckCount()).isEqualTo(1);
        assertThat(stats.totalCards()).isEqualTo(12);
    }

    private List<Object[]> chunk(String content) {
        List<Object[]> list = new ArrayList<>();
        list.add(new Object[]{UUID.randomUUID(), content});
        return list;
    }
}
