package com.financeapp.dto;

import java.math.BigDecimal;

public class FinancialSettingsDtos {

    public record RatesRequest(
            BigDecimal savingsRate,
            BigDecimal cdbRate,
            BigDecimal treasuryRate,
            BigDecimal fixedIncomeRate,
            BigDecimal fiiRate,
            BigDecimal stocksRate
    ) {}

    public record RatesResponse(
            BigDecimal savingsRate,
            BigDecimal cdbRate,
            BigDecimal treasuryRate,
            BigDecimal fixedIncomeRate,
            BigDecimal fiiRate,
            BigDecimal stocksRate,
            String disclaimer
    ) {}
}
