package ianlegaria.personalknowledgeengine.quiz.repository;

import ianlegaria.personalknowledgeengine.quiz.entity.QuizQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestionEntity, UUID> {

    List<QuizQuestionEntity> findBySessionIdOrderByQuestionIndexAsc(UUID sessionId);

    Optional<QuizQuestionEntity> findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(UUID sessionId);
}
