package com.financeapp.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceMathTest {

    @Test
    void annualToMonthlyRateUsesCompoundConversionNotSimpleDivision() {
        // 12,68% a.a. equivale a exatamente 1,00% a.m. na conversão composta:
        // (1 + 0,01)^12 - 1 = 0,126825...
        BigDecimal monthly = FinanceMath.annualToMonthlyRate(new BigDecimal("12.6825"));
        assertThat(monthly.doubleValue()).isCloseTo(0.01, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void simulateWithNoContributionMatchesClassicCompoundInterestFormula() {
        // M = P × (1 + i)^n - sem aportes, 1000 investidos a 12% a.a. por 12 meses
        var result = FinanceMath.simulate(new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("12"), 12);
        BigDecimal finalBalance = result.get(11).balance();

        // (1+0.12)^1 aplicado via taxa mensal composta por 12 meses = 1.12 do valor inicial
        assertThat(finalBalance.doubleValue()).isCloseTo(1120.0, org.assertj.core.data.Offset.offset(1.0));
    }

    @Test
    void simulateAccumulatesContributionsCorrectly() {
        var result = FinanceMath.simulate(new BigDecimal("0"), new BigDecimal("100"), BigDecimal.ZERO, 10);
        // taxa zero: patrimônio final = soma simples dos aportes
        assertThat(result.get(9).balance()).isEqualByComparingTo("1000.00");
        assertThat(result.get(9).invested()).isEqualByComparingTo("1000.00");
    }

    @Test
    void pricePaymentMatchesKnownFinancingExample() {
        // financiamento de 10.000, taxa de 1% ao mês, 12 parcelas
        // PMT = 10000 * 0.01 / (1 - 1.01^-12) ≈ 888.49
        BigDecimal payment = FinanceMath.pricePayment(new BigDecimal("10000"), new BigDecimal("0.01"), 12);
        assertThat(payment.doubleValue()).isCloseTo(888.49, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void pricePaymentWithZeroRateJustDividesEqually() {
        BigDecimal payment = FinanceMath.pricePayment(new BigDecimal("1200"), BigDecimal.ZERO, 12);
        assertThat(payment).isEqualByComparingTo("100.00");
    }
}
