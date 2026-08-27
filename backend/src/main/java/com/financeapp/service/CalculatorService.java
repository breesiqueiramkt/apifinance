package com.financeapp.service;

import com.financeapp.dto.CalculatorDtos.*;
import com.financeapp.util.FinanceMath;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CalculatorService {

    public CompoundInterestResponse compoundInterest(CompoundInterestRequest req) {
        var months = FinanceMath.simulate(req.initialValue(), req.monthlyContribution(), req.annualRate(), req.months());

        List<MonthlyPoint> evolution = months.stream()
                .map(m -> new MonthlyPoint(m.month(), m.invested(), m.balance()))
                .toList();

        var last = months.get(months.size() - 1);
        BigDecimal totalInvested = last.invested();
        BigDecimal finalAmount = last.balance();
        BigDecimal totalReturns = finalAmount.subtract(totalInvested);

        return new CompoundInterestResponse(totalInvested, totalReturns, finalAmount, evolution);
    }

    public FinancialIndependenceResponse financialIndependence(FinancialIndependenceRequest req) {
        // patrimônio necessário para os rendimentos anuais cobrirem os gastos anuais
        BigDecimal annualExpenses = req.monthlyExpenses().multiply(BigDecimal.valueOf(12));
        BigDecimal requiredNetWorth = annualExpenses
                .divide(req.expectedRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP), 2, RoundingMode.HALF_UP);

        // simula até 80 anos (960 meses) de aportes para estimar quando chega lá
        var months = FinanceMath.simulate(req.currentNetWorth(), req.monthlyContribution(), req.expectedRate(), 960);
        Integer monthsToReach = months.stream()
                .filter(m -> m.balance().compareTo(requiredNetWorth) >= 0)
                .map(FinanceMath.MonthResult::month)
                .findFirst()
                .orElse(null);

        Double years = monthsToReach != null ? Math.round((monthsToReach / 12.0) * 10) / 10.0 : null;

        return new FinancialIndependenceResponse(FinanceMath.round(requiredNetWorth), monthsToReach, years);
    }

    public EmergencyFundResponse emergencyFund(EmergencyFundRequest req) {
        BigDecimal amount = req.monthlyExpenses().multiply(BigDecimal.valueOf(req.months()));
        return new EmergencyFundResponse(FinanceMath.round(amount));
    }

    public InflationResponse inflation(InflationRequest req) {
        double inflation = req.annualInflation().doubleValue() / 100.0;
        double years = req.years();

        // poder de compra futuro do valor de hoje
        double purchasingPower = req.currentValue().doubleValue() / Math.pow(1 + inflation, years);
        // quanto em R$ nominais será preciso no futuro para comprar o que currentValue compra hoje
        double nominalNeeded = req.currentValue().doubleValue() * Math.pow(1 + inflation, years);

        return new InflationResponse(
                FinanceMath.round(BigDecimal.valueOf(purchasingPower)),
                FinanceMath.round(BigDecimal.valueOf(nominalNeeded))
        );
    }

    public FinancingResponse financing(FinancingRequest req) {
        BigDecimal financedAmount = req.assetValue().subtract(req.downPayment());
        BigDecimal monthlyRate = FinanceMath.annualToMonthlyRate(req.annualRate());
        BigDecimal installmentValue = FinanceMath.pricePayment(financedAmount, monthlyRate, req.installmentsCount());
        BigDecimal totalPaid = installmentValue.multiply(BigDecimal.valueOf(req.installmentsCount()));
        BigDecimal totalInterest = totalPaid.subtract(financedAmount);

        return new FinancingResponse(
                FinanceMath.round(financedAmount), installmentValue,
                FinanceMath.round(totalPaid), FinanceMath.round(totalInterest)
        );
    }

    public RetirementResponse retirement(RetirementRequest req) {
        int months = (req.retirementAge() - req.currentAge()) * 12;
        if (months <= 0) {
            BigDecimal netWorth = req.currentNetWorth();
            return new RetirementResponse(netWorth, estimateMonthlyIncome(netWorth));
        }

        var simulation = FinanceMath.simulate(req.currentNetWorth(), req.monthlyContribution(), req.expectedRate(), months);
        BigDecimal projected = simulation.get(simulation.size() - 1).balance();

        return new RetirementResponse(projected, estimateMonthlyIncome(projected));
    }

    /** Estimativa conservadora de renda mensal na aposentadoria pela "regra dos 4% ao ano". */
    private BigDecimal estimateMonthlyIncome(BigDecimal netWorth) {
        BigDecimal annualWithdrawal = netWorth.multiply(new BigDecimal("0.04"));
        return FinanceMath.round(annualWithdrawal.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP));
    }
}
