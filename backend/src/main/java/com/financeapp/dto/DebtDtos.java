package com.financeapp.dto;

import com.financeapp.entity.DebtStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DebtDtos {

    public record DebtRequest(
            @NotBlank(message = "Credor é obrigatório") String creditor,
            @NotNull @DecimalMin(value = "0.01") BigDecimal originalAmount,
            @NotNull BigDecimal currentAmount,
            BigDecimal interestRate,
            Short installmentsTotal,
            Short installmentsPaid,
            LocalDate dueDate,
            DebtStatus status
    ) {}

    public record DebtResponse(
            Long id,
            String creditor,
            BigDecimal originalAmount,
            BigDecimal currentAmount,
            BigDecimal paidAmount,
            BigDecimal interestRate,
            Short installmentsTotal,
            Short installmentsPaid,
            LocalDate dueDate,
            DebtStatus status,
            BigDecimal estimatedInstallmentValue,
            BigDecimal estimatedRemainingInterest
    ) {}

    public record DebtSummary(
            BigDecimal totalOriginal,
            BigDecimal totalPaid,
            BigDecimal totalRemaining
    ) {}
}
