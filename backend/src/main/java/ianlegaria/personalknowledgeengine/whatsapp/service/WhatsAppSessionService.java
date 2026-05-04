package ianlegaria.personalknowledgeengine.whatsapp.service;

import ianlegaria.personalknowledgeengine.whatsapp.dto.WhatsAppSession;

import java.util.Optional;

public interface WhatsAppSessionService {

    void save(String chatId, WhatsAppSession session);

    Optional<WhatsAppSession> get(String chatId);

    void delete(String chatId);
}
