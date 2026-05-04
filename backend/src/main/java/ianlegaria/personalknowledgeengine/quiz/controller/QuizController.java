package ianlegaria.personalknowledgeengine.quiz.controller;

import ianlegaria.personalknowledgeengine.quiz.dto.*;
import ianlegaria.personalknowledgeengine.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Tag(name = "Quiz", description = "LLM-generated quizzes with automatic grading")
@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "Start quiz session",
            description = """
                    Generates quiz questions from the specified documents using Cohere Command-R.
                    Supports two grading modes:
                    - STRICT: the answer must match the expected answer exactly.
                    - OPEN: the answer is evaluated semantically by the LLM (partial credit possible).
                    Returns a session with all generated questions.
                    """)
    @ApiResponse(responseCode = "201", description = "Quiz session created")
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizSessionResponse startQuiz(@Valid @RequestBody StartQuizRequest req) {
        return quizService.startQuiz(req);
    }

    @Operation(summary = "Submit answer",
            description = """
                    Evaluates the submitted answer using Cohere LLM (in OPEN mode) or exact matching (in STRICT mode).
                    Returns feedback including correctness, the expected answer, and an explanation.
                    The session is marked COMPLETED when the last question is answered.
                    Returns 400 if the session is already completed.
                    Send Accept: application/json for a synchronous response.
                    """)
    @ApiResponse(responseCode = "200", description = "Answer evaluated")
    @ApiResponse(responseCode = "400", description = "Session already completed")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @PostMapping(value = "/sessions/{sessionId}/answer", produces = MediaType.APPLICATION_JSON_VALUE)
    public AnswerFeedbackResponse submitAnswer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest req) {
        return quizService.submitAnswer(sessionId, req);
    }

    @Operation(summary = "Submit answer (streaming)",
            description = """
                    Same as POST /sessions/{sessionId}/answer but streams the evaluation feedback token by token via SSE.
                    Send Accept: text/event-stream to activate streaming on the same endpoint.
                    Events:
                    - {"type":"token","text":"..."} — feedback token
                    - {"type":"complete","verdict":"CORRECT|PARTIALLY_CORRECT|INCORRECT","isCorrect":bool,
                       "sessionComplete":bool,"finalScore":null|int,"totalQuestions":null|int,"nextQuestion":null|{...}}
                    """)
    @PostMapping(value = "/sessions/{sessionId}/answer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> submitAnswerStream(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest req) {
        return quizService.submitAnswerStream(sessionId, req);
    }

    @Operation(summary = "Get quiz session")
    @ApiResponse(responseCode = "200", description = "Session found")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @GetMapping("/sessions/{sessionId}")
    public QuizSessionResponse getSession(@PathVariable UUID sessionId) {
        return quizService.getSession(sessionId);
    }
}
