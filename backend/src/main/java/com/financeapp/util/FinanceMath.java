package com.financeapp.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Funções financeiras puras, sem dependência de banco ou de request/response.
 * Ficam isoladas aqui para poderem ser testadas isoladamente (seção 26 do
 * briefing pede explicitamente que os cálculos financeiros sejam testados)
 * e reaproveitadas pelas calculadoras, por Investimentos e por Dívidas.
 */
public final class FinanceMath {

    private static final MathContext MC = new MathContext(12);

    private FinanceMath() {}

    /**
     * Converte uma taxa anual (%) para a taxa mensal equivalente (%),
     * usando capitalização composta: (1 + i_ano)^(1/12) - 1.
     * Essa é a forma matematicamente correta de "quebrar" uma taxa anual em
     * mensal (a divisão simples por 12 subestima o efeito dos juros compostos).
     */
    public static BigDecimal annualToMonthlyRate(BigDecimal annualRatePercent) {
        double annual = annualRatePercent.doubleValue() / 100.0;
        double monthly = Math.pow(1 + annual, 1.0 / 12.0) - 1;
        return BigDecimal.valueOf(monthly);
    }

    /**
     * Simula patrimônio mês a mês com aportes mensais fixos e juros compostos.
     * M = P × (1 + i)^n aplicado mês a mês, com aporte somado a cada período
     * (fórmula de série de pagamentos), conforme pedido na seção 28.
     */
    public static java.util.List<MonthResult> simulate(
            BigDecimal initialValue, BigDecimal monthlyContribution, BigDecimal annualRatePercent, int months) {

        BigDecimal monthlyRate = annualToMonthlyRate(annualRatePercent);
        java.util.List<MonthResult> results = new java.util.ArrayList<>();

        BigDecimal balance = initialValue;
        BigDecimal invested = initialValue;

        for (int m = 1; m <= months; m++) {
            balance = balance.multiply(BigDecimal.ONE.add(monthlyRate), MC).add(monthlyContribution);
            invested = invested.add(monthlyContribution);
            results.add(new MonthResult(m, round(invested), round(balance)));
        }
        return results;
    }

    /**
     * Valor da parcela fixa de um financiamento pela Tabela Price:
     * PMT = PV × i / (1 - (1 + i)^-n)
     */
    public static BigDecimal pricePayment(BigDecimal presentValue, BigDecimal monthlyRate, int installments) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return round(presentValue.divide(BigDecimal.valueOf(installments), MC));
        }
        double pv = presentValue.doubleValue();
        double i = monthlyRate.doubleValue();
        double pmt = pv * i / (1 - Math.pow(1 + i, -installments));
        return round(BigDecimal.valueOf(pmt));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record MonthResult(int month, BigDecimal invested, BigDecimal balance) {}
}
