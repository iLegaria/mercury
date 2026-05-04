package ianlegaria.personalknowledgeengine.flashcard;

import ianlegaria.personalknowledgeengine.flashcard.dto.ImportResult;
import ianlegaria.personalknowledgeengine.flashcard.dto.ReviewResult;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardDeckEntity;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardDeckRepository;
import ianlegaria.personalknowledgeengine.flashcard.service.FlashcardServiceImpl;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock FlashcardDeckRepository deckRepository;
    @Mock FlashcardCardRepository cardRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FlashcardServiceImpl flashcardService;

    // ── SM-2 tests ───────────────────────────────────────────────────────────

    @Test
    void applySmTwo_quality0_resetsIntervalToOne() {
        FlashcardCardEntity card = cardWithDefaults(5, 2.5, 20);

        invokeApplySmTwo(card, 0);

        assertThat(card.getRepetitions()).isEqualTo(0);
        assertThat(card.getIntervalDays()).isEqualTo(1);
        assertThat(card.getEaseFactor()).isLessThan(2.5); // EF decreases
        assertThat(card.getNextReviewAt()).isNotNull();
    }

    @Test
    void applySmTwo_quality3_firstReview_intervalIs1() {
        FlashcardCardEntity card = cardWithDefaults(0, 2.5, 0);

        invokeApplySmTwo(card, 3);

        assertThat(card.getRepetitions()).isEqualTo(1);
        assertThat(card.getIntervalDays()).isEqualTo(1);
    }

    @Test
    void applySmTwo_quality5_secondReview_intervalIs6() {
        FlashcardCardEntity card = cardWithDefaults(1, 2.5, 1);

        invokeApplySmTwo(card, 5);

        assertThat(card.getRepetitions()).isEqualTo(2);
        assertThat(card.getIntervalDays()).isEqualTo(6);
        assertThat(card.getEaseFactor()).isGreaterThan(2.5); // EF increases with quality 5
    }

    // ── importCards tests ────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void importCards_validCsvWithEmptyRow_importsCorrectlyAndSkipsEmpty() {
        UUID deckId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("a@b.com").name("A").build();
        FlashcardDeckEntity deck = FlashcardDeckEntity.builder()
                .id(deckId).user(user).name("Test Deck").build();

        when(deckRepository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(deck));
        when(cardRepository.countByDeckId(deckId)).thenReturn(0L);
        when(cardRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        String csv = "question,answer\n" +
                "Q1,A1\n" +
                "Q2,A2\n" +
                "Q3,A3\n" +
                ",\n";  // empty row — should be skipped

        MockMultipartFile file = new MockMultipartFile(
                "file", "cards.csv", "text/csv", csv.getBytes());

        ImportResult result = flashcardService.importCards(deckId, userId, file);

        assertThat(result.imported()).isEqualTo(3);
        assertThat(result.skipped()).isEqualTo(1);

        ArgumentCaptor<List<FlashcardCardEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(cardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(3);
    }

    @Test
    void applySmTwo_repeatedFailures_easeFactorNeverFallsBelowMinimum() {
        FlashcardCardEntity card = cardWithDefaults(5, 2.5, 20);

        for (int i = 0; i < 10; i++) {
            invokeApplySmTwo(card, 0);
        }

        assertThat(card.getEaseFactor()).isGreaterThanOrEqualTo(1.3);
    }

    // ── reviewCard tests ─────────────────────────────────────────────────────

    @Test
    void reviewCard_quality3_savesCardAndReturnsSchedule() {
        UUID deckId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        FlashcardDeckEntity deck = FlashcardDeckEntity.builder().id(deckId).build();
        FlashcardCardEntity card = cardWithDefaults(0, 2.5, 0);
        card.setDeck(deck);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(deckRepository.findByIdAndUserId(deckId, userId)).thenReturn(Optional.of(deck));
        when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReviewResult result = flashcardService.reviewCard(cardId, userId, 3);

        assertThat(result.nextReviewAt()).isNotNull();
        assertThat(result.intervalDays()).isEqualTo(1); // first review, quality≥3 → interval=1
        verify(cardRepository).save(card);
    }

    // ── addQuizMistake tests ─────────────────────────────────────────────────

    @Test
    void addQuizMistake_deckAbsent_createsQuizMistakesDeck() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("x@b.com").name("X").build();
        UUID newDeckId = UUID.randomUUID();
        FlashcardDeckEntity savedDeck = FlashcardDeckEntity.builder()
                .id(newDeckId).user(user).name("Quiz Mistakes").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deckRepository.findByUserIdAndName(userId, "Quiz Mistakes")).thenReturn(Optional.empty());
        when(deckRepository.save(any())).thenReturn(savedDeck);
        when(cardRepository.countByDeckId(newDeckId)).thenReturn(0L);
        when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String question = "What is the difference between RAG and fine-tuning?";
        String answer = "RAG retrieves context at inference time; fine-tuning bakes knowledge into weights.";

        flashcardService.addQuizMistake(userId, question, answer);

        verify(deckRepository).save(argThat(d -> "Quiz Mistakes".equals(d.getName())));
        verify(cardRepository).save(argThat(c -> question.equals(c.getQuestion()) && answer.equals(c.getAnswer())));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void invokeApplySmTwo(FlashcardCardEntity card, int quality) {
        ReflectionTestUtils.invokeMethod(flashcardService, "applySmTwo", card, quality);
    }

    private FlashcardCardEntity cardWithDefaults(int repetitions, double easeFactor, int intervalDays) {
        FlashcardCardEntity card = new FlashcardCardEntity();
        card.setRepetitions(repetitions);
        card.setEaseFactor(easeFactor);
        card.setIntervalDays(intervalDays);
        return card;
    }
}
