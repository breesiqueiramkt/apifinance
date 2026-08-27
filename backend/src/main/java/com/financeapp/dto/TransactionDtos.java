package com.financeapp.dto;

import com.financeapp.entity.RecurrenceType;
import com.financeapp.entity.TransactionStatus;
import com.financeapp.entity.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDtos {

    public record TransactionRequest(
            @NotNull(message = "Conta é obrigatória") Long accountId,
            Long categoryId,
            @NotNull(message = "Tipo é obrigatório (INCOME ou EXPENSE)") TransactionType type,
            @NotBlank(message = "Descrição é obrigatória") String description,
            @NotNull @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero") BigDecimal amount,
            @NotNull(message = "Data é obrigatória") LocalDate date,
            String paymentMethod,
            TransactionStatus status,
            RecurrenceType recurrence,
            String notes
    ) {}

    public record TransactionResponse(
            Long id,
            Long accountId,
            String accountName,
            Long categoryId,
            String categoryName,
            TransactionType type,
            String description,
            BigDecimal amount,
            LocalDate date,
            String paymentMethod,
            TransactionStatus status,
            RecurrenceType recurrence,
            String notes
    ) {}
}
