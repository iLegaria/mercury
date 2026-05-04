package ianlegaria.personalknowledgeengine.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSessionEntity session;

    @Column(nullable = false, columnDefinition = "text")
    private String questionText;

    @Column(nullable = false, columnDefinition = "text")
    private String contextUsed;

    @Column(nullable = false)
    private int questionIndex;

    @Column(columnDefinition = "text")
    private String userAnswer;

    @Column(columnDefinition = "text")
    private String feedback;

    private Boolean isCorrect;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;
}
