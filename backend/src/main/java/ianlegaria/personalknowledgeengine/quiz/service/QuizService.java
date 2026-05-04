package ianlegaria.personalknowledgeengine.quiz.service;

import ianlegaria.personalknowledgeengine.quiz.dto.AnswerFeedbackResponse;
import ianlegaria.personalknowledgeengine.quiz.dto.QuizSessionResponse;
import ianlegaria.personalknowledgeengine.quiz.dto.StartQuizRequest;
import ianlegaria.personalknowledgeengine.quiz.dto.SubmitAnswerRequest;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface QuizService {

    QuizSessionResponse startQuiz(StartQuizRequest req);

    AnswerFeedbackResponse submitAnswer(UUID sessionId, SubmitAnswerRequest req);

    QuizSessionResponse getSession(UUID sessionId);

    Flux<String> submitAnswerStream(UUID sessionId, SubmitAnswerRequest req);
}
