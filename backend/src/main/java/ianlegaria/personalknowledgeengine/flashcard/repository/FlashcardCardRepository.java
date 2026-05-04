package ianlegaria.personalknowledgeengine.flashcard.repository;

import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FlashcardCardRepository extends JpaRepository<FlashcardCardEntity, UUID> {
    List<FlashcardCardEntity> findByDeckIdOrderByCardIndexAsc(UUID deckId);
    long countByDeckId(UUID deckId);

    @Query("SELECT c FROM FlashcardCardEntity c WHERE c.deck.id = :deckId AND (c.nextReviewAt IS NULL OR c.nextReviewAt <= :now) ORDER BY c.nextReviewAt ASC NULLS FIRST")
    List<FlashcardCardEntity> findDueCards(@Param("deckId") UUID deckId, @Param("now") OffsetDateTime now);

    @Query("SELECT COUNT(c) FROM FlashcardCardEntity c WHERE c.deck.id = :deckId AND (c.nextReviewAt IS NULL OR c.nextReviewAt <= :now)")
    long countDueByDeckId(@Param("deckId") UUID deckId, @Param("now") OffsetDateTime now);

    @Query("SELECT c FROM FlashcardCardEntity c WHERE c.deck.user.id = :userId AND (c.nextReviewAt IS NULL OR c.nextReviewAt <= :now) ORDER BY c.nextReviewAt ASC NULLS FIRST")
    List<FlashcardCardEntity> findDueCardsByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
