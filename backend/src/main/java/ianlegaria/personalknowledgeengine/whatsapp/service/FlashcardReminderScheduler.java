package ianlegaria.personalknowledgeengine.whatsapp.service;

import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardDeckEntity;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardDeckRepository;
import ianlegaria.personalknowledgeengine.whatsapp.dto.WhatsAppSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashcardReminderScheduler {

    private final FlashcardCardRepository cardRepository;
    private final FlashcardDeckRepository deckRepository;
    private final WhatsAppService whatsAppService;
    private final WhatsAppSessionService sessionService;

    @Value("${whatsapp.enabled:false}")
    private boolean enabled;

    /** Green API chatId format: "521234567890@c.us" */
    @Value("${whatsapp.user-chat-id:}")
    private String userChatId;

    @Value("${whatsapp.user-id:}")
    private String userIdStr;

    @Value("${whatsapp.max-cards-per-session:10}")
    private int maxCards;

    @Scheduled(cron = "${whatsapp.reminder-cron:0 0 8 * * *}")
    public void sendDueFlashcards() {
        log.info("WhatsApp reminder triggered — enabled={}, chatId={}, userId={}", enabled, userChatId, userIdStr);
        if (!enabled || userChatId.isBlank() || userIdStr.isBlank()) {
            log.warn("WhatsApp reminder skipped — enabled={}, chatId blank={}, userId blank={}",
                    enabled, userChatId.isBlank(), userIdStr.isBlank());
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid WHATSAPP_USER_ID: {}", userIdStr);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<FlashcardCardEntity> due = resolveDueCards(userId, now);

        if (due.isEmpty()) {
            whatsAppService.sendMessage(userChatId, "No flashcards due today. Keep it up!");
            return;
        }

        List<UUID> cardIds = due.stream().map(FlashcardCardEntity::getId).toList();
        sessionService.save(userChatId, WhatsAppSession.builder()
                .userId(userId)
                .cardIds(cardIds)
                .currentIndex(0)
                .build());

        whatsAppService.sendMessage(userChatId, formatQuestion(due.get(0), 1, due.size()));
        log.info("Sent {} due flashcards to {}", due.size(), userChatId);
    }

    private List<FlashcardCardEntity> resolveDueCards(UUID userId, OffsetDateTime now) {
        List<FlashcardDeckEntity> enabledDecks = deckRepository.findByUserIdAndWhatsappReminderEnabledTrue(userId);

        if (enabledDecks.isEmpty()) {
            // No decks configured → use all decks (backward compatible default)
            return cardRepository.findDueCardsByUserId(userId, now)
                    .stream().limit(maxCards).toList();
        }

        return enabledDecks.stream()
                .flatMap(deck -> cardRepository.findDueCards(deck.getId(), now).stream())
                .sorted(Comparator.comparing(c -> c.getNextReviewAt() == null
                        ? OffsetDateTime.MIN : c.getNextReviewAt()))
                .limit(maxCards)
                .toList();
    }

    public static String formatQuestion(FlashcardCardEntity card, int current, int total) {
        return String.format("*Flashcard %d/%d*\n\n%s", current, total, card.getQuestion());
    }
}
