package com.financeapp.service;

import com.financeapp.dto.CalculatorDtos.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorServiceTest {

    private final CalculatorService service = new CalculatorService();

    @Test
    void emergencyFundMultipliesMonthlyExpensesByChosenMonths() {
        var response = service.emergencyFund(new EmergencyFundRequest(new BigDecimal("2500"), 6));
        // exemplo citado no próprio briefing: R$ 2.500 x 6 meses = R$ 15.000
        assertThat(response.recommendedAmount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void compoundInterestSeparatesInvestedAmountFromReturns() {
        var response = service.compoundInterest(
                new CompoundInterestRequest(new BigDecimal("1000"), new BigDecimal("200"), new BigDecimal("10"), 24));

        BigDecimal expectedInvested = new BigDecimal("1000").add(new BigDecimal("200").multiply(BigDecimal.valueOf(24)));
        assertThat(response.totalInvested()).isEqualByComparingTo(expectedInvested);
        // com taxa positiva, o rendimento não pode ser zero nem negativo
        assertThat(response.totalReturns()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.finalAmount()).isEqualByComparingTo(response.totalInvested().add(response.totalReturns()));
    }

    @Test
    void financingWithZeroInterestJustDividesAssetMinusDownPayment() {
        var response = service.financing(
                new FinancingRequest(new BigDecimal("12000"), new BigDecimal("2000"), BigDecimal.ZERO, 10));

        assertThat(response.financedAmount()).isEqualByComparingTo("10000.00");
        assertThat(response.installmentValue()).isEqualByComparingTo("1000.00");
        assertThat(response.totalInterest()).isEqualByComparingTo("0.00");
    }

    @Test
    void inflationReducesFuturePurchasingPowerOfTodaysMoney() {
        var response = service.inflation(new InflationRequest(new BigDecimal("1000"), new BigDecimal("5"), 10));
        // com inflação positiva, o poder de compra futuro é sempre menor que o valor nominal de hoje
        assertThat(response.futurePurchasingPower()).isLessThan(new BigDecimal("1000"));
        // e o valor nominal necessário no futuro é sempre maior que hoje
        assertThat(response.futureNominalValueNeeded()).isGreaterThan(new BigDecimal("1000"));
    }

    @Test
    void financialIndependenceRequiredNetWorthFollowsPerpetuityRule() {
        // gasto mensal 5000 -> anual 60000; taxa esperada 6% a.a.
        // patrimônio necessário = 60000 / 0.06 = 1.000.000
        var response = service.financialIndependence(
                new FinancialIndependenceRequest(new BigDecimal("5000"), BigDecimal.ZERO, new BigDecimal("6"), new BigDecimal("2000")));

        assertThat(response.requiredNetWorth()).isEqualByComparingTo("1000000.00");
    }
}
