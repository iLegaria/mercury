package ianlegaria.personalknowledgeengine.flashcard.service;

import ianlegaria.personalknowledgeengine.common.exception.DuplicateResourceException;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.flashcard.dto.*;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardDeckEntity;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardDeckRepository;
import ianlegaria.personalknowledgeengine.quiz.event.QuizAnswerIncorrectEvent;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardCardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeckResponse createDeck(CreateDeckRequest req) {
        UserEntity user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.userId()));

        String trimmedName = req.name().trim();
        if (deckRepository.existsByUserIdAndName(req.userId(), trimmedName)) {
            throw new DuplicateResourceException("Deck '" + trimmedName + "' already exists for this user");
        }

        FlashcardDeckEntity deck = deckRepository.save(FlashcardDeckEntity.builder()
                .user(user)
                .name(trimmedName)
                .description(req.description())
                .build());

        log.info("Created flashcard deck '{}' for user {}", trimmedName, req.userId());
        return toResponse(deck, 0L, 0L);
    }

    @Transactional(readOnly = true)
    public Page<DeckResponse> getDecksByUser(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        OffsetDateTime now = OffsetDateTime.now();
        return deckRepository.findByUserId(userId, pageable)
                .map(d -> toResponse(d,
                        cardRepository.countByDeckId(d.getId()),
                        cardRepository.countDueByDeckId(d.getId(), now)));
    }

    @Transactional
    public void deleteDeck(UUID deckId, UUID userId) {
        FlashcardDeckEntity deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        deckRepository.delete(deck);
        log.info("Deleted flashcard deck {} for user {}", deckId, userId);
    }

    @Transactional
    public ImportResult importCards(UUID deckId, UUID userId, MultipartFile file) {
        FlashcardDeckEntity deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        int nextIndex = (int) cardRepository.countByDeckId(deckId);
        int imported = 0;
        int skipped = 0;
        List<FlashcardCardEntity> toSave = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                if (record.size() < 2) {
                    skipped++;
                    continue;
                }
                String question = record.get(0).trim();
                String answer = record.get(1).trim();
                if (question.isEmpty() || answer.isEmpty()) {
                    skipped++;
                    continue;
                }
                toSave.add(FlashcardCardEntity.builder()
                        .deck(deck)
                        .question(question)
                        .answer(answer)
                        .cardIndex(nextIndex++)
                        .build());
                imported++;
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse CSV: " + e.getMessage());
        }

        cardRepository.saveAll(toSave);
        log.info("Imported {} cards into deck {} ({} skipped)", imported, deckId, skipped);
        return new ImportResult(imported, skipped);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getCards(UUID deckId, UUID userId) {
        deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        return cardRepository.findByDeckIdOrderByCardIndexAsc(deckId).stream()
                .map(this::toCardResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getDueCards(UUID deckId, UUID userId) {
        deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        return cardRepository.findDueCards(deckId, OffsetDateTime.now()).stream()
                .map(this::toCardResponse)
                .toList();
    }

    @Transactional
    public ReviewResult reviewCard(UUID cardId, UUID userId, int quality) {
        FlashcardCardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));

        // Validate ownership via deck
        deckRepository.findByIdAndUserId(card.getDeck().getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));

        applySmTwo(card, quality);
        cardRepository.save(card);

        log.info("Reviewed card {} with quality={}, next review in {} days", cardId, quality, card.getIntervalDays());
        return new ReviewResult(card.getNextReviewAt(), card.getIntervalDays());
    }

    @Transactional
    public void addQuizMistake(UUID userId, String question, String answer) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        FlashcardDeckEntity deck = deckRepository.findByUserIdAndName(userId, "Quiz Mistakes")
                .orElseGet(() -> deckRepository.save(FlashcardDeckEntity.builder()
                        .user(user)
                        .name("Quiz Mistakes")
                        .description("Cards auto-generated from incorrect or partial quiz answers")
                        .build()));

        int nextIndex = (int) cardRepository.countByDeckId(deck.getId());
        cardRepository.save(FlashcardCardEntity.builder()
                .deck(deck)
                .question(question)
                .answer(answer)
                .cardIndex(nextIndex)
                .build());

        log.info("Added quiz-mistake card to deck '{}' for user {}", deck.getId(), userId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQuizMistake(QuizAnswerIncorrectEvent event) {
        addQuizMistake(event.userId(), event.question(), event.correctAnswer());
    }

    // SM-2 algorithm
    private void applySmTwo(FlashcardCardEntity card, int quality) {
        int n = card.getRepetitions();
        double ef = card.getEaseFactor();
        int prevInterval = card.getIntervalDays();
        int interval;

        if (quality < 3) {
            n = 0;
            interval = 1;
        } else {
            if (n == 0) {
                interval = 1;
            } else if (n == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(prevInterval * ef);
            }
            n++;
        }

        ef = ef + 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02);
        if (ef < 1.3) ef = 1.3;

        card.setRepetitions(n);
        card.setEaseFactor(ef);
        card.setIntervalDays(interval);
        card.setNextReviewAt(OffsetDateTime.now().plusDays(interval));
    }

    @Transactional
    public CardResponse addCard(CreateCardRequest req) {
        FlashcardDeckEntity deck = deckRepository.findByIdAndUserId(req.deckId(), req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
        int nextIndex = (int) cardRepository.countByDeckId(req.deckId());
        FlashcardCardEntity card = cardRepository.save(FlashcardCardEntity.builder()
                .deck(deck)
                .question(req.question().trim())
                .answer(req.answer().trim())
                .cardIndex(nextIndex)
                .build());
        log.info("Added card to deck {} for user {}", req.deckId(), req.userId());
        return toCardResponse(card);
    }

    @Transactional
    public CardResponse updateCard(UUID cardId, UUID userId, String question, String answer) {
        FlashcardCardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        deckRepository.findByIdAndUserId(card.getDeck().getId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
        card.setQuestion(question.trim());
        card.setAnswer(answer.trim());
        log.info("Updated card {} for user {}", cardId, userId);
        return toCardResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(UUID cardId, UUID userId) {
        FlashcardCardEntity card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        deckRepository.findByIdAndUserId(card.getDeck().getId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
        cardRepository.delete(card);
        log.info("Deleted card {} for user {}", cardId, userId);
    }

    @Transactional
    public void setWhatsappReminder(UUID deckId, UUID userId, boolean enabled) {
        FlashcardDeckEntity deck = deckRepository.findByIdAndUserId(deckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        deck.setWhatsappReminderEnabled(enabled);
        deckRepository.save(deck);
        log.info("WhatsApp reminder {} for deck {} (user {})", enabled ? "enabled" : "disabled", deckId, userId);
    }

    private DeckResponse toResponse(FlashcardDeckEntity deck, long cardCount, long dueCount) {
        return new DeckResponse(
                deck.getId(),
                deck.getUser().getId(),
                deck.getName(),
                deck.getDescription(),
                cardCount,
                dueCount,
                deck.getCreatedAt(),
                deck.isWhatsappReminderEnabled()
        );
    }

    private CardResponse toCardResponse(FlashcardCardEntity card) {
        return new CardResponse(
                card.getId(),
                card.getDeck().getId(),
                card.getQuestion(),
                card.getAnswer(),
                card.getCardIndex(),
                card.getNextReviewAt(),
                card.getIntervalDays(),
                card.getRepetitions(),
                card.getEaseFactor()
        );
    }
}
