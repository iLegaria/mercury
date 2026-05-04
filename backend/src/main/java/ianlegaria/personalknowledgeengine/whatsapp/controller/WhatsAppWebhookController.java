package ianlegaria.personalknowledgeengine.whatsapp.controller;

import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import ianlegaria.personalknowledgeengine.flashcard.repository.FlashcardCardRepository;
import ianlegaria.personalknowledgeengine.flashcard.service.FlashcardService;
import ianlegaria.personalknowledgeengine.whatsapp.dto.GreenApiWebhookPayload;
import ianlegaria.personalknowledgeengine.whatsapp.dto.WhatsAppSession;
import ianlegaria.personalknowledgeengine.whatsapp.service.FlashcardReminderScheduler;
import ianlegaria.personalknowledgeengine.whatsapp.service.WhatsAppService;
import ianlegaria.personalknowledgeengine.whatsapp.service.WhatsAppSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppSessionService sessionService;
    private final WhatsAppService whatsAppService;
    private final FlashcardCardRepository cardRepository;
    private final FlashcardService flashcardService;
    private final CohereClient cohereClient;

    @Value("${green-api.webhook-token:}")
    private String webhookToken;

    @Value("${whatsapp.user-chat-id:}")
    private String userChatId;

    @Value("${whatsapp.max-message-age-seconds:180}")
    private long maxMessageAgeSeconds;

    private static final String EVAL_PROMPT = """
            You are evaluating a flashcard answer. Given the question and the accepted answer(s), \
            determine if the student's reply is correct.

            Respond on two lines:
            - First line: CORRECT or INCORRECT (nothing else)
            - Second line: one short sentence of feedback (what was right or what the correct answer is)

            Be lenient with formatting, capitalization, and minor wording differences. \
            If multiple answers are accepted (separated by |), any one of them is sufficient.""";

    @PostMapping
    public ResponseEntity<Void> handleIncoming(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody GreenApiWebhookPayload payload) {

        if (!webhookToken.isBlank()) {
            String expected = "Bearer " + webhookToken;
            if (!expected.equals(authHeader)) {
                log.warn("WhatsApp webhook rejected — invalid token");
                return ResponseEntity.status(401).build();
            }
        }

        if (payload.getTimestamp() != null) {
            long ageSeconds = Instant.now().getEpochSecond() - payload.getTimestamp();
            if (ageSeconds > maxMessageAgeSeconds) {
                log.debug("Skipping stale WhatsApp message ({}s old, id={})", ageSeconds, payload.getIdMessage());
                return ResponseEntity.ok().build();
            }
        }

        String type = payload.getTypeWebhook();
        boolean isUserReply = "incomingMessageReceived".equals(type)
                || "outgoingMessageReceived".equals(type);
        if (!isUserReply) {
            return ResponseEntity.ok().build();
        }

        if (payload.getMessageData() == null
                || !"textMessage".equals(payload.getMessageData().getTypeMessage())
                || payload.getMessageData().getTextMessageData() == null) {
            return ResponseEntity.ok().build();
        }

        String chatId = payload.getSenderData().getChatId();
        String body = payload.getMessageData().getTextMessageData().getTextMessage();
        log.info("WhatsApp message from {}: {}", chatId, body);

        // Ignore messages from chats that are not the configured user session
        if (!userChatId.isBlank() && !userChatId.equals(chatId)) {
            log.debug("Ignoring webhook for unrelated chat {}", chatId);
            return ResponseEntity.ok().build();
        }

        Optional<WhatsAppSession> sessionOpt = sessionService.get(chatId);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        WhatsAppSession session = sessionOpt.get();
        UUID cardId = session.getCardIds().get(session.getCurrentIndex());

        FlashcardCardEntity card = cardRepository.findById(cardId).orElse(null);
        if (card == null) {
            sessionService.delete(chatId);
            whatsAppService.sendMessage(chatId, "Session error. Please try again later.");
            return ResponseEntity.ok().build();
        }

        String userMessage = String.format(
                "Question: %s\nAccepted answer(s): %s\nStudent's reply: %s",
                card.getQuestion(), card.getAnswer(), body.trim());
        String evaluation = cohereClient.chat(EVAL_PROMPT, userMessage);
        String[] lines = evaluation.split("\n", 2);
        boolean correct = lines[0].trim().toUpperCase().startsWith("CORRECT");
        String feedback = lines.length > 1 ? lines[1].trim() : "";

        try {
            flashcardService.reviewCard(cardId, session.getUserId(), correct ? 5 : 1);
        } catch (Exception e) {
            log.warn("SM-2 update failed for card {}: {}", cardId, e.getMessage());
        }

        int nextIndex = session.getCurrentIndex() + 1;
        int total = session.getCardIds().size();
        boolean done = nextIndex >= total;

        StringBuilder reply = new StringBuilder();
        reply.append(correct ? "✓ Correct!" : "✗ Incorrect.");
        if (!feedback.isBlank()) reply.append(" ").append(feedback);

        if (done) {
            sessionService.delete(chatId);
            reply.append(String.format("\n\nSession complete! Reviewed %d card%s.", total, total == 1 ? "" : "s"));
        } else {
            session.setCurrentIndex(nextIndex);
            sessionService.save(chatId, session);
            FlashcardCardEntity next = cardRepository.findById(session.getCardIds().get(nextIndex)).orElse(null);
            if (next != null) {
                reply.append("\n\n").append(FlashcardReminderScheduler.formatQuestion(next, nextIndex + 1, total));
            }
        }

        whatsAppService.sendMessage(chatId, reply.toString());
        return ResponseEntity.ok().build();
    }
}
