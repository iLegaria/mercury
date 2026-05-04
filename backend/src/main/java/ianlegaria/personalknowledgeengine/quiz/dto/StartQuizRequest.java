package ianlegaria.personalknowledgeengine.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartQuizRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private UUID collectionId; // null = use all user's documents

    @Min(value = 3, message = "Minimum 3 questions")
    @Max(value = 15, message = "Maximum 15 questions")
    @Builder.Default
    private int numQuestions = 5;
}
