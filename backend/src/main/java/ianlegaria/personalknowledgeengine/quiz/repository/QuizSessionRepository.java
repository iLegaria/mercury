package ianlegaria.personalknowledgeengine.quiz.repository;

import ianlegaria.personalknowledgeengine.quiz.entity.QuizSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizSessionRepository extends JpaRepository<QuizSessionEntity, UUID> {

    List<QuizSessionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<QuizSessionEntity> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
