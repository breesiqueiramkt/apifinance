package com.financeapp.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class CalculatorDtos {

    // ---------- Juros compostos / aportes ----------

    public record CompoundInterestRequest(
            @NotNull @DecimalMin(value = "0") BigDecimal initialValue,
            @NotNull @DecimalMin(value = "0") BigDecimal monthlyContribution,
            @NotNull @DecimalMin(value = "0") BigDecimal annualRate,
            @NotNull @Min(1) @Max(600) Integer months
    ) {}

    public record MonthlyPoint(int month, BigDecimal invested, BigDecimal balance) {}

    public record CompoundInterestResponse(
            BigDecimal totalInvested,
            BigDecimal totalReturns,
            BigDecimal finalAmount,
            List<MonthlyPoint> evolution
    ) {}

    // ---------- Independência financeira ----------

    public record FinancialIndependenceRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal monthlyExpenses,
            @NotNull @DecimalMin(value = "0") BigDecimal currentNetWorth,
            @NotNull @DecimalMin(value = "0.01") BigDecimal expectedRate,
            @NotNull @DecimalMin(value = "0") BigDecimal monthlyContribution
    ) {}

    public record FinancialIndependenceResponse(
            BigDecimal requiredNetWorth,   // patrimônio necessário p/ viver dos rendimentos
            Integer monthsToReach,         // null se nunca alcançar no horizonte simulado
            Double yearsToReach
    ) {}

    // ---------- Reserva de emergência ----------

    public record EmergencyFundRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal monthlyExpenses,
            @NotNull @Min(1) @Max(24) Integer months
    ) {}

    public record EmergencyFundResponse(BigDecimal recommendedAmount) {}

    // ---------- Inflação ----------

    public record InflationRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal currentValue,
            @NotNull @DecimalMin(value = "0") BigDecimal annualInflation,
            @NotNull @Min(1) @Max(80) Integer years
    ) {}

    public record InflationResponse(
            BigDecimal futurePurchasingPower,     // quanto os R$ de hoje vão "valer" daqui a X anos
            BigDecimal futureNominalValueNeeded    // quantos R$ nominais você vai precisar pra ter o mesmo poder de compra de hoje
    ) {}

    // ---------- Financiamento ----------

    public record FinancingRequest(
            @NotNull @DecimalMin(value = "0.01") BigDecimal assetValue,
            @NotNull @DecimalMin(value = "0") BigDecimal downPayment,
            @NotNull @DecimalMin(value = "0") BigDecimal annualRate,
            @NotNull @Min(1) @Max(480) Integer installmentsCount
    ) {}

    public record FinancingResponse(
            BigDecimal financedAmount,
            BigDecimal installmentValue,
            BigDecimal totalPaid,
            BigDecimal totalInterest
    ) {}

    // ---------- Aposentadoria ----------

    public record RetirementRequest(
            @NotNull @Min(14) @Max(100) Integer currentAge,
            @NotNull @Min(15) @Max(100) Integer retirementAge,
            @NotNull @DecimalMin(value = "0") BigDecimal currentNetWorth,
            @NotNull @DecimalMin(value = "0") BigDecimal monthlyContribution,
            @NotNull @DecimalMin(value = "0.01") BigDecimal expectedRate
    ) {}

    public record RetirementResponse(
            BigDecimal projectedNetWorth,
            BigDecimal estimatedMonthlyIncome // regra dos 4% ao ano, dividida por 12
    ) {}
}
