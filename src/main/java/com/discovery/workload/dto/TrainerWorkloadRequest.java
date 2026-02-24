package com.discovery.workload.dto;

import com.discovery.workload.model.ActionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerWorkloadRequest {

    @NotBlank(message = "TrainingId is required")
    @Size(max = 80, message = "TrainingId must be at most 80 characters")
    private String trainingId;

    @NotBlank(message = "Trainer username is required")
    @Size(max = 80)
    private String username;

    @NotBlank(message = "Trainer first name is required")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "Trainer last name is required")
    @Size(max = 80)
    private String lastName;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    @NotNull(message = "Training date is required")
    private LocalDate trainingDate;

    @NotNull(message = "Training duration is required")
    @Positive(message = "Training duration must be positive")
    private Integer trainingDurationMinutes;

    @NotNull(message = "Action type is required")
    private ActionType actionType;
}

