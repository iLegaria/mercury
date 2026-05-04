package ianlegaria.personalknowledgeengine.whatsapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.whatsapp.dto.WhatsAppSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppSessionServiceImpl implements WhatsAppSessionService {

    private static final String KEY_PREFIX = "whatsapp:session:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(String chatId, WhatsAppSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key(chatId), json, TTL);
        } catch (Exception e) {
            log.error("Failed to save WhatsApp session for {}: {}", chatId, e.getMessage());
        }
    }

    public Optional<WhatsAppSession> get(String chatId) {
        try {
            String json = redisTemplate.opsForValue().get(key(chatId));
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, WhatsAppSession.class));
        } catch (Exception e) {
            log.error("Failed to read WhatsApp session for {}: {}", chatId, e.getMessage());
            return Optional.empty();
        }
    }

    public void delete(String chatId) {
        redisTemplate.delete(key(chatId));
    }

    private String key(String chatId) {
        return KEY_PREFIX + chatId;
    }
}
