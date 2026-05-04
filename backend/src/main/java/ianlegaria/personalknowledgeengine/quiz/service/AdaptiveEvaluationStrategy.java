package ianlegaria.personalknowledgeengine.quiz.service;

import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdaptiveEvaluationStrategy implements QuizEvaluationStrategy {

    private final CohereClient cohereClient;

    @Override
    public QuizMode supportedMode() {
        return QuizMode.ADAPTIVE;
    }

    @Override
    public String systemPrompt() {
        return """
                You are a rigorous study tutor evaluating a student's answer. \
                Use the provided context as your source of truth. Be honest — do not reward \
                vague or tangentially related answers with partial credit.

                Evaluate the answer on a 3-tier scale and respond in this exact format:
                - First line: one of CORRECT, PARTIALLY_CORRECT, or INCORRECT (nothing else on this line)
                - Remaining lines: 3-5 sentences of educational feedback

                Grading criteria — apply these strictly:
                - CORRECT: directly addresses the question and captures the core concept accurately. \
                Minor imprecision is acceptable if the key idea is clearly demonstrated.
                - PARTIALLY_CORRECT: ONLY use this when the student has genuinely addressed part of \
                the question correctly but is missing another significant part. The answer must show \
                real understanding of at least one relevant aspect. Do NOT use PARTIALLY_CORRECT for \
                answers that are vague, off-topic, or only tangentially related to what was asked.
                - INCORRECT: the answer is wrong, does not address the actual question, is too vague \
                to demonstrate understanding, or only mentions a surface-level detail that misses \
                the concept being tested.

                When in doubt between PARTIALLY_CORRECT and INCORRECT, ask: "Did the student show \
                they understand any specific part of what the question is asking?" If no, use INCORRECT.

                Your feedback must:
                1. State clearly what the correct answer is based on the context
                2. Explain specifically what was right, wrong, or missing in the student's response
                3. Add relevant context from the document to deepen understanding

                Answer in the same language the student used.""";
    }

    @Override
    public EvaluationResult evaluate(String question, String userAnswer, String context) {
        String userMessage = String.format(
                "Context:\n%s\n\nQuestion: %s\n\nStudent's answer: %s",
                context, question, userAnswer
        );
        String response = cohereClient.chat(systemPrompt(), userMessage);
        String[] lines = response.split("\n", 2);
        String feedback = lines.length > 1 ? lines[1].trim() : response;
        return EvaluationResult.parse(lines[0], feedback);
    }
}
