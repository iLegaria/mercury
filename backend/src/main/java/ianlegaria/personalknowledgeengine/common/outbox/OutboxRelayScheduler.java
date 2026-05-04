package ianlegaria.personalknowledgeengine.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxEventSaved(OutboxEventSavedEvent event) {
        relay();
    }

    @Scheduled(fixedDelay = 300000)
    public void relay() {
        List<OutboxEvent> pending = outboxEventRepository.findByProcessedAtIsNullOrderByCreatedAtAsc();
        if (pending.isEmpty()) return;

        log.debug("Relaying {} outbox event(s)", pending.size());

        for (OutboxEvent event : pending) {
            try {
                Object payload = objectMapper.readValue(event.getPayload(), Class.forName(event.getEventType()));
                rabbitTemplate.convertAndSend(event.getExchange(), event.getRoutingKey(), payload);
                event.setProcessedAt(OffsetDateTime.now());
                outboxEventRepository.save(event);
                log.debug("Relayed outbox event {} ({})", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to relay outbox event {} ({}): {}", event.getId(), event.getEventType(), e.getMessage());
            }
        }
    }
}
