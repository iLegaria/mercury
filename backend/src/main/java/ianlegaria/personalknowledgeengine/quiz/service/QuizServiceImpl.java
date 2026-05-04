package ianlegaria.personalknowledgeengine.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.collection.entity.CollectionEntity;
import ianlegaria.personalknowledgeengine.collection.repository.CollectionRepository;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.common.exception.ResourceNotFoundException;
import ianlegaria.personalknowledgeengine.quiz.dto.*;
import ianlegaria.personalknowledgeengine.quiz.entity.QuizQuestionEntity;
import ianlegaria.personalknowledgeengine.quiz.entity.QuizSessionEntity;
import ianlegaria.personalknowledgeengine.quiz.event.QuizAnswerIncorrectEvent;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizQuestionRepository;
import ianlegaria.personalknowledgeengine.quiz.repository.QuizSessionRepository;
import ianlegaria.personalknowledgeengine.user.entity.UserEntity;
import ianlegaria.personalknowledgeengine.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuizServiceImpl implements QuizService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final ChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final CollectionRepository collectionRepository;
    private final CohereClient cohereClient;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Map<QuizMode, QuizEvaluationStrategy> strategies;

    public QuizServiceImpl(
            QuizSessionRepository quizSessionRepository,
            QuizQuestionRepository quizQuestionRepository,
            ChunkRepository chunkRepository,
            UserRepository userRepository,
            CollectionRepository collectionRepository,
            CohereClient cohereClient,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher,
            List<QuizEvaluationStrategy> strategyList) {
        this.quizSessionRepository = quizSessionRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.collectionRepository = collectionRepository;
        this.cohereClient = cohereClient;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(QuizEvaluationStrategy::supportedMode, s -> s));
    }

    @Transactional
    public QuizSessionResponse startQuiz(StartQuizRequest req) {
        UserEntity user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getUserId()));

        CollectionEntity collection = null;
        if (req.getCollectionId() != null) {
            collection = collectionRepository.findByIdAndUserId(req.getCollectionId(), req.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
        }

        List<Object[]> chunks = req.getCollectionId() != null
                ? chunkRepository.findRandomChunksInCollection(req.getUserId(), req.getCollectionId(), req.getNumQuestions())
                : chunkRepository.findRandomChunks(req.getUserId(), req.getNumQuestions());

        if (chunks.size() < req.getNumQuestions()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Not enough content to generate " + req.getNumQuestions() + " questions. Upload more documents first.");
        }

        // Generate questions via LLM before saving (fail fast before touching DB)
        List<String[]> generated = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String content = (String) chunks.get(i)[1];
            log.info("Generating question {}/{}", i + 1, req.getNumQuestions());
            String questionText = generateQuestion(content);
            generated.add(new String[]{questionText, content});
        }

        // Persist session
        QuizSessionEntity session = quizSessionRepository.save(
                QuizSessionEntity.builder()
                        .user(user)
                        .collection(collection)
                        .status("ACTIVE")
                        .mode(QuizMode.ADAPTIVE.name())
                        .totalQuestions(req.getNumQuestions())
                        .correctAnswers(0)
                        .build()
        );

        // Persist questions
        List<QuizQuestionEntity> questions = new ArrayList<>();
        for (int i = 0; i < generated.size(); i++) {
            questions.add(QuizQuestionEntity.builder()
                    .session(session)
                    .questionText(generated.get(i)[0])
                    .contextUsed(generated.get(i)[1])
                    .questionIndex(i)
                    .build());
        }
        quizQuestionRepository.saveAll(questions);

        log.info("Quiz session {} created with {} questions", session.getId(), req.getNumQuestions());

        QuizQuestionEntity first = questions.get(0);
        return new QuizSessionResponse(
                session.getId(),
                session.getTotalQuestions(),
                session.getCorrectAnswers(),
                session.getStatus(),
                session.getMode(),
                new QuizQuestionDto(first.getId(), first.getQuestionText(), 0, req.getNumQuestions())
        );
    }

    @Transactional
    public AnswerFeedbackResponse submitAnswer(UUID sessionId, SubmitAnswerRequest req) {
        QuizSessionEntity session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz session not found: " + sessionId));

        if (!session.getUser().getId().equals(req.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if ("COMPLETED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quiz session is already completed");
        }

        QuizQuestionEntity question = quizQuestionRepository
                .findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No unanswered questions remaining"));

        QuizEvaluationStrategy strategy = resolveStrategy(session.getMode());
        EvaluationResult result = strategy.evaluate(
                question.getQuestionText(), req.getAnswer(), question.getContextUsed());

        question.setUserAnswer(req.getAnswer());
        question.setFeedback(result.feedback());
        question.setIsCorrect(result.isCorrect());
        question.setAnsweredAt(OffsetDateTime.now());
        quizQuestionRepository.save(question);

        if (result.isCorrect()) {
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
        } else {
            applicationEventPublisher.publishEvent(
                    new QuizAnswerIncorrectEvent(req.getUserId(), question.getQuestionText(), result.feedback()));
        }

        QuizQuestionEntity next = quizQuestionRepository
                .findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId)
                .orElse(null);

        boolean sessionComplete = next == null;
        if (sessionComplete) {
            session.setStatus("COMPLETED");
        }
        quizSessionRepository.save(session);

        QuizQuestionDto nextDto = next == null ? null : new QuizQuestionDto(
                next.getId(), next.getQuestionText(), next.getQuestionIndex(), session.getTotalQuestions()
        );

        return new AnswerFeedbackResponse(
                result.isCorrect(),
                result.verdict(),
                result.feedback(),
                sessionComplete,
                sessionComplete ? session.getCorrectAnswers() : null,
                sessionComplete ? session.getTotalQuestions() : null,
                nextDto
        );
    }

    @Transactional(readOnly = true)
    public QuizSessionResponse getSession(UUID sessionId) {
        QuizSessionEntity session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz session not found: " + sessionId));

        QuizQuestionEntity current = quizQuestionRepository
                .findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId)
                .orElse(null);

        QuizQuestionDto currentDto = current == null ? null : new QuizQuestionDto(
                current.getId(), current.getQuestionText(), current.getQuestionIndex(), session.getTotalQuestions()
        );

        return new QuizSessionResponse(
                session.getId(),
                session.getTotalQuestions(),
                session.getCorrectAnswers(),
                session.getStatus(),
                session.getMode(),
                currentDto
        );
    }

    // ── Streaming evaluation ──────────────────────────────────────────────────

    public Flux<String> submitAnswerStream(UUID sessionId, SubmitAnswerRequest req) {
        QuizSessionEntity session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz session not found: " + sessionId));

        if (!session.getUser().getId().equals(req.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if ("COMPLETED".equals(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quiz session is already completed");
        }

        QuizQuestionEntity question = quizQuestionRepository
                .findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No unanswered questions remaining"));

        QuizEvaluationStrategy strategy = resolveStrategy(session.getMode());
        UUID questionId = question.getId();
        String systemPrompt = strategy.systemPrompt();
        String userMessage = String.format("Context:\n%s\n\nQuestion: %s\n\nStudent's answer: %s",
                question.getContextUsed(), question.getQuestionText(), req.getAnswer());

        AtomicBoolean verdictFound = new AtomicBoolean(false);
        StringBuilder verdictBuf = new StringBuilder();
        StringBuilder feedbackBuf = new StringBuilder();

        Flux<String> tokenStream = cohereClient.chatStream(systemPrompt, userMessage)
                .concatMap(token -> {
                    if (verdictFound.get()) {
                        feedbackBuf.append(token);
                        return Flux.just(tokenEvent(token));
                    }
                    verdictBuf.append(token);
                    int idx = verdictBuf.indexOf("\n");
                    if (idx < 0) return Flux.empty();
                    verdictFound.set(true);
                    String after = verdictBuf.substring(idx + 1);
                    if (!after.isEmpty()) {
                        feedbackBuf.append(after);
                        return Flux.just(tokenEvent(after));
                    }
                    return Flux.empty();
                });

        Mono<String> completeEvent = Mono.fromCallable(() -> {
            String rawVerdictLine = verdictBuf.toString().split("\n", 2)[0];
            EvaluationResult result = EvaluationResult.parse(rawVerdictLine, feedbackBuf.toString().trim());

            return transactionTemplate.execute(status -> {
                QuizSessionEntity s = quizSessionRepository.findById(sessionId).orElseThrow();
                QuizQuestionEntity q = quizQuestionRepository.findById(questionId).orElseThrow();

                q.setUserAnswer(req.getAnswer());
                q.setFeedback(result.feedback());
                q.setIsCorrect(result.isCorrect());
                q.setAnsweredAt(OffsetDateTime.now());
                quizQuestionRepository.save(q);

                if (result.isCorrect()) {
                    s.setCorrectAnswers(s.getCorrectAnswers() + 1);
                } else {
                    applicationEventPublisher.publishEvent(
                            new QuizAnswerIncorrectEvent(s.getUser().getId(), q.getQuestionText(), result.feedback()));
                }

                QuizQuestionEntity next = quizQuestionRepository
                        .findFirstBySessionIdAndUserAnswerIsNullOrderByQuestionIndexAsc(sessionId)
                        .orElse(null);
                boolean complete = next == null;
                if (complete) s.setStatus("COMPLETED");
                quizSessionRepository.save(s);

                QuizQuestionDto nextDto = next == null ? null : new QuizQuestionDto(
                        next.getId(), next.getQuestionText(), next.getQuestionIndex(), s.getTotalQuestions());

                return buildCompleteEvent(result.verdict(), result.isCorrect(), complete,
                        complete ? s.getCorrectAnswers() : null,
                        complete ? s.getTotalQuestions() : null,
                        nextDto);
            });
        }).subscribeOn(Schedulers.boundedElastic());

        return Flux.concat(tokenStream, completeEvent.flux());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private QuizEvaluationStrategy resolveStrategy(String mode) {
        QuizMode quizMode = QuizMode.ADAPTIVE;
        try {
            quizMode = QuizMode.valueOf(mode);
        } catch (IllegalArgumentException ignored) {}
        QuizEvaluationStrategy strategy = strategies.get(quizMode);
        if (strategy == null) strategy = strategies.get(QuizMode.ADAPTIVE);
        if (strategy == null) throw new IllegalStateException("No evaluation strategy found for mode: " + mode);
        return strategy;
    }

    private String generateQuestion(String chunkContent) {
        String systemPrompt = """
                You are a study quiz generator. Create ONE question based on the provided text \
                that tests genuine understanding — prefer questions that require the student to \
                explain, apply, or reason about a concept, not just recall a fact.
                Rules:
                1. The question must be answerable from the provided text.
                2. Return ONLY the question text. No preamble, no numbering, no explanation.""";

        String userMessage = String.format(
                "Generate one study question based on this text:\n\n---\n%s\n---",
                chunkContent
        );
        return cohereClient.chat(systemPrompt, userMessage);
    }

    private String tokenEvent(String text) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", "token", "text", text));
        } catch (Exception e) {
            return "{\"type\":\"token\",\"text\":\"\"}";
        }
    }

    private String buildCompleteEvent(String verdict, boolean isCorrect, boolean sessionComplete,
                                      Integer finalScore, Integer totalQuestions, QuizQuestionDto nextQuestion) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "complete");
            event.put("verdict", verdict);
            event.put("isCorrect", isCorrect);
            event.put("sessionComplete", sessionComplete);
            event.put("finalScore", finalScore);
            event.put("totalQuestions", totalQuestions);
            event.put("nextQuestion", nextQuestion);
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return "{\"type\":\"complete\"}";
        }
    }
}
