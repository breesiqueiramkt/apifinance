package com.financeapp.dto;

import com.financeapp.entity.BillStatus;
import com.financeapp.entity.RecurrenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillDtos {

    public record BillRequest(
            @NotBlank(message = "Descrição é obrigatória") String description,
            @NotNull @jakarta.validation.constraints.DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull(message = "Vencimento é obrigatório") LocalDate dueDate,
            Long accountId,
            Long categoryId,
            RecurrenceType recurrence
    ) {}

    public record BillResponse(
            Long id,
            String description,
            BigDecimal amount,
            LocalDate dueDate,
            Long accountId,
            String accountName,
            Long categoryId,
            String categoryName,
            RecurrenceType recurrence,
            BillStatus status,
            long daysUntilDue
    ) {}
}
