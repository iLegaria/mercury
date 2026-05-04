package ianlegaria.personalknowledgeengine.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAnswerRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Answer is required")
    private String answer;
}
