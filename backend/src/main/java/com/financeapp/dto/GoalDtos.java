package com.financeapp.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GoalDtos {

    public record GoalRequest(
            @NotBlank(message = "Nome da meta é obrigatório") String name,
            @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate deadline
    ) {}

    public record ContributeRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {}

    public record GoalResponse(
            Long id,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate deadline,
            BigDecimal progressPercent,
            BigDecimal monthlyContributionNeeded,
            Long monthsRemaining
    ) {}
}
