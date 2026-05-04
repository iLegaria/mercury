package ianlegaria.personalknowledgeengine.flashcard.repository;

import ianlegaria.personalknowledgeengine.flashcard.entity.FlashcardDeckEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeckEntity, UUID> {
    Page<FlashcardDeckEntity> findByUserId(UUID userId, Pageable pageable);
    Optional<FlashcardDeckEntity> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndName(UUID userId, String name);
    Optional<FlashcardDeckEntity> findByUserIdAndName(UUID userId, String name);
    List<FlashcardDeckEntity> findBySourceDocumentId(UUID sourceDocumentId);
    List<FlashcardDeckEntity> findByUserIdAndWhatsappReminderEnabledTrue(UUID userId);
}
