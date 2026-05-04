package ianlegaria.personalknowledgeengine.quiz;

import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.collection.repository.CollectionRepository;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.quiz.dto.AnswerFeedbackResponse;
import ianlegaria.personalknowledgeengine.quiz.dto.StartQuizRequest;
import ianlegaria.personalknowledgeengine.quiz.dto.SubmitAnswerRequest;
import ianlegaria.personalknowledgeengine.quiz.entity.QuizQuestionEntity;
import ianlegaria.personalknowledgeengine.quiz.entity.QuizSessionEntity;
import ianlegaria.personalknowledgeengine.quiz.event.QuizAnswerIncorrectEvent;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizQuestionRepository;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizSessionRepository;
import ianlegaria.personalknowledgeengine.quiz.service.AdaptiveEvaluationStrategy;
import ianlegaria.personalknowledgeengine.quiz.service.QuizServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock QuizSessionRepository quizSessionRepository;
    @Mock QuizQuestionRepository quizQuestionRepository;
    @Mock ChunkRepository chunkRepository;
    @Mock UserRepository userRepository;
    @Mock CollectionRepository collectionRepository;
    @Mock CohereClient cohereClient;
    @Mock TransactionTemplate transactionTemplate;
    @Mock ApplicationEventPublisher applicationEventPublisher;

    private QuizServiceImpl quizService;

    @BeforeEach
    void setUp() {
        AdaptiveEvaluationStrategy adaptive = new AdaptiveEvaluationStrategy(cohereClient);
        quizService = new QuizServiceImpl(
                quizSessionRepository, quizQuestionRepository, chunkRepository,
                userRepository, collectionRepository, cohereClient,
                transactionTemplate, new ObjectMapper(), applicationEventPublisher,
                List.of(adaptive));
    }

    @Test
    void submitAnswer_sessionAlreadyCompleted_throwsConflict() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("a@b.com").name("A").build();

        QuizSessionEntity session = QuizSessionEntity.builder()
                .id(sessionId).user(user).status("COMPLETED").mode("STRICT")
                .totalQuestions(1).correctAnswers(1).build();

        when(quizSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        SubmitAnswerRequest req = new SubmitAnswerRequest(userId, "my answer");

        assertThatThrownBy(() -> quizService.submitAnswer(sessionId, req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(quizQuestionRepository, never()).findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(any());
    }

    @Test
    void submitAnswer_lastQuestion_completesSession() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("b@b.com").name("B").build();

        QuizSessionEntity session = QuizSessionEntity.builder()
                .id(sessionId).user(user).status("ACTIVE").mode("STRICT")
                .totalQuestions(1).correctAnswers(0).build();
        when(quizSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(quizSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizQuestionEntity question = QuizQuestionEntity.builder()
                .id(UUID.randomUUID()).session(session).questionIndex(0)
                .questionText("What is RAG?").contextUsed("RAG stands for Retrieval Augmented Generation").build();

        when(quizQuestionRepository.findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId))
                .thenReturn(Optional.of(question))
                .thenReturn(Optional.empty());
        when(quizQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(cohereClient.chat(anyString(), anyString())).thenReturn("CORRECT\nGood answer!");

        SubmitAnswerRequest req = new SubmitAnswerRequest(userId, "Retrieval Augmented Generation");

        AnswerFeedbackResponse response = quizService.submitAnswer(sessionId, req);

        assertThat(response.sessionComplete()).isTrue();
        assertThat(response.isCorrect()).isTrue();
        assertThat(session.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void submitAnswer_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(quizSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizService.submitAnswer(sessionId, new SubmitAnswerRequest(UUID.randomUUID(), "answer")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void submitAnswer_incorrectAnswer_publishesQuizAnswerIncorrectEvent() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("c@b.com").name("C").build();

        QuizSessionEntity session = QuizSessionEntity.builder()
                .id(sessionId).user(user).status("ACTIVE").mode("ADAPTIVE")
                .totalQuestions(2).correctAnswers(0).build();
        when(quizSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(quizSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizQuestionEntity question = QuizQuestionEntity.builder()
                .id(UUID.randomUUID()).session(session).questionIndex(0)
                .questionText("What is semantic search?").contextUsed("Semantic search uses vectors.").build();

        when(quizQuestionRepository.findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId))
                .thenReturn(Optional.of(question))
                .thenReturn(Optional.empty());
        when(quizQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cohereClient.chat(anyString(), anyString())).thenReturn("INCORRECT\nYour answer missed the key points.");

        quizService.submitAnswer(sessionId, new SubmitAnswerRequest(userId, "I don't know"));

        verify(applicationEventPublisher).publishEvent(any(QuizAnswerIncorrectEvent.class));
    }

    @Test
    void submitAnswer_notLastQuestion_returnsNextQuestion() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("d@b.com").name("D").build();

        QuizSessionEntity session = QuizSessionEntity.builder()
                .id(sessionId).user(user).status("ACTIVE").mode("ADAPTIVE")
                .totalQuestions(2).correctAnswers(0).build();
        when(quizSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(quizSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        QuizQuestionEntity q1 = QuizQuestionEntity.builder()
                .id(UUID.randomUUID()).session(session).questionIndex(0)
                .questionText("First question").contextUsed("context1").build();
        QuizQuestionEntity q2 = QuizQuestionEntity.builder()
                .id(UUID.randomUUID()).session(session).questionIndex(1)
                .questionText("Second question").contextUsed("context2").build();

        when(quizQuestionRepository.findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId))
                .thenReturn(Optional.of(q1))
                .thenReturn(Optional.of(q2));
        when(quizQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cohereClient.chat(anyString(), anyString())).thenReturn("CORRECT\nGood job!");

        AnswerFeedbackResponse response = quizService.submitAnswer(sessionId, new SubmitAnswerRequest(userId, "my answer"));

        assertThat(response.sessionComplete()).isFalse();
        assertThat(response.nextQuestion()).isNotNull();
        assertThat(response.nextQuestion().questionText()).isEqualTo("Second question");
    }

    @Test
    void startQuiz_notEnoughChunks_throwsBadRequest() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("e@b.com").name("E").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chunkRepository.findRandomChunks(userId, 3)).thenReturn(List.of());

        StartQuizRequest req = StartQuizRequest.builder().userId(userId).numQuestions(3).build();

        assertThatThrownBy(() -> quizService.startQuiz(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
