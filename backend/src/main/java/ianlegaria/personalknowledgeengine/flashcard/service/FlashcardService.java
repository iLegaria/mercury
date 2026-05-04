package ianlegaria.personalknowledgeengine.flashcard.service;

import ianlegaria.personalknowledgeengine.flashcard.dto.CardResponse;
import ianlegaria.personalknowledgeengine.flashcard.dto.CreateCardRequest;
import ianlegaria.personalknowledgeengine.flashcard.dto.CreateDeckRequest;
import ianlegaria.personalknowledgeengine.flashcard.dto.DeckResponse;
import ianlegaria.personalknowledgeengine.flashcard.dto.ImportResult;
import ianlegaria.personalknowledgeengine.flashcard.dto.ReviewResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FlashcardService {

    DeckResponse createDeck(CreateDeckRequest req);

    Page<DeckResponse> getDecksByUser(UUID userId, Pageable pageable);

    void deleteDeck(UUID deckId, UUID userId);

    ImportResult importCards(UUID deckId, UUID userId, MultipartFile file);

    List<CardResponse> getCards(UUID deckId, UUID userId);

    List<CardResponse> getDueCards(UUID deckId, UUID userId);

    ReviewResult reviewCard(UUID cardId, UUID userId, int quality);

    void addQuizMistake(UUID userId, String question, String answer);

    CardResponse addCard(CreateCardRequest req);

    CardResponse updateCard(UUID cardId, UUID userId, String question, String answer);

    void deleteCard(UUID cardId, UUID userId);

    void setWhatsappReminder(UUID deckId, UUID userId, boolean enabled);
}
