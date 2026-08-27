package com.financeapp.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDtos {

    public record CategoryBreakdown(Long categoryId, String categoryName, String icon, BigDecimal total) {}

    public record UpcomingBill(Long transactionId, String description, BigDecimal amount, String dueDate) {}

    public record DashboardResponse(
            BigDecimal netWorth,              // patrimônio total = contas + investimentos
            BigDecimal available,             // disponível = soma das contas (dinheiro líquido)
            BigDecimal investedTotal,          // total investido (valor atual dos investimentos)
            BigDecimal monthlyIncome,          // receita do mês corrente
            BigDecimal monthlyExpenses,        // despesas do mês corrente
            BigDecimal previousMonthNetWorth,  // patrimônio no fim do mês anterior, para calcular variação
            BigDecimal projectedBalance,       // receitas previstas - despesas previstas (inclui pendentes/agendadas)
            BigDecimal savingsRate,            // (receita - despesa) / receita * 100
            BigDecimal expenseCommitment,      // despesa / receita * 100
            List<CategoryBreakdown> expensesByCategory,
            List<UpcomingBill> upcomingPending  // lançamentos PENDING/SCHEDULED dos próximos 30 dias
    ) {}
}
