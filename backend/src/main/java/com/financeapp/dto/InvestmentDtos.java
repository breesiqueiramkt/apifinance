package com.financeapp.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvestmentDtos {

    public record InvestmentRequest(
            @NotBlank(message = "Nome do investimento é obrigatório") String name,
            Long investmentTypeId,
            @NotNull @DecimalMin(value = "0.01") BigDecimal investedAmount,
            @NotNull BigDecimal currentAmount,
            @NotNull LocalDate investedAt,
            BigDecimal expectedRate,
            String institution,
            String notes
    ) {}

    public record InvestmentResponse(
            Long id,
            String name,
            Long investmentTypeId,
            String investmentTypeName,
            BigDecimal investedAmount,
            BigDecimal currentAmount,
            BigDecimal returnAmount,     // currentAmount - investedAmount
            BigDecimal returnPercent,    // returnAmount / investedAmount * 100
            LocalDate investedAt,
            BigDecimal expectedRate,
            String institution,
            String notes
    ) {}

    public record InvestmentTypeResponse(Long id, String name, String investmentClass) {}

    public record InvestmentSummary(
            BigDecimal totalInvested,
            BigDecimal totalCurrent,
            BigDecimal totalReturn,
            BigDecimal totalReturnPercent,
            BigDecimal estimatedMonthlyIncome,
            BigDecimal estimatedYearlyIncome
    ) {}
}
