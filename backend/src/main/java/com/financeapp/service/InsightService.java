package com.financeapp.service;

import com.financeapp.dto.InsightDtos.InsightResponse;
import com.financeapp.entity.*;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.InvestmentRepository;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.util.FinanceMath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InsightService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final InvestmentRepository investmentRepository;
    private final CategoryRepository categoryRepository;
    private final FinancialSettingsService financialSettingsService;

    private static final List<TransactionStatus> REALIZED = List.of(TransactionStatus.PAID);

    public InsightResponse build(Long userId) {
        List<String> insights = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate thisStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate thisEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate prevMonth = today.minusMonths(1);
        LocalDate prevStart = prevMonth.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate prevEnd = prevMonth.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal income = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                userId, TransactionType.INCOME, REALIZED, thisStart, thisEnd);
        BigDecimal expenses = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                userId, TransactionType.EXPENSE, REALIZED, thisStart, thisEnd);

        addCategoryChangeInsight(insights, userId, thisStart, thisEnd, prevStart, prevEnd);
        addFixedExpensesInsight(insights, userId, thisStart, thisEnd, income);
        addSavingsRateInsight(insights, income, expenses);
        addInvestmentShareInsight(insights, userId);
        addProjectionInsight(insights, userId, income, expenses);

        if (insights.isEmpty()) {
            insights.add("Ainda não há lançamentos suficientes este mês para gerar uma análise. Continue registrando suas receitas e despesas.");
        }

        return new InsightResponse(insights);
    }

    private void addCategoryChangeInsight(List<String> insights, Long userId, LocalDate thisStart, LocalDate thisEnd,
                                           LocalDate prevStart, LocalDate prevEnd) {
        Map<Long, BigDecimal> current = groupByCategory(
                transactionRepository.sumGroupedByCategory(userId, TransactionType.EXPENSE, thisStart, thisEnd));
        Map<Long, BigDecimal> previous = groupByCategory(
                transactionRepository.sumGroupedByCategory(userId, TransactionType.EXPENSE, prevStart, prevEnd));

        String biggestIncreaseCategory = null;
        BigDecimal biggestIncreasePct = BigDecimal.ZERO;

        for (var entry : current.entrySet()) {
            BigDecimal prevValue = previous.get(entry.getKey());
            if (prevValue == null || prevValue.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal change = entry.getValue().subtract(prevValue)
                    .multiply(BigDecimal.valueOf(100)).divide(prevValue, 2, RoundingMode.HALF_UP);

            if (change.compareTo(biggestIncreasePct) > 0) {
                biggestIncreasePct = change;
                biggestIncreaseCategory = categoryName(entry.getKey());
            }
        }

        if (biggestIncreaseCategory != null && biggestIncreasePct.compareTo(BigDecimal.TEN) >= 0) {
            insights.add(String.format(Locale.of("pt", "BR"),
                    "Você gastou %.0f%% a mais com %s este mês em comparação ao mês anterior.",
                    biggestIncreasePct, biggestIncreaseCategory.toLowerCase()));
        }
    }

    private void addFixedExpensesInsight(List<String> insights, Long userId, LocalDate start, LocalDate end, BigDecimal income) {
        if (income.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal fixedExpenses = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end)
                .stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> t.getStatus() == TransactionStatus.PAID)
                .filter(t -> t.getRecurrence() != RecurrenceType.NONE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (fixedExpenses.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal pct = fixedExpenses.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP);
        insights.add("Seus gastos fixos (recorrentes) representam " + pct + "% da sua renda deste mês.");
    }

    private void addSavingsRateInsight(List<String> insights, BigDecimal income, BigDecimal expenses) {
        if (income.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal savingsRate = income.subtract(expenses).multiply(BigDecimal.valueOf(100))
                .divide(income, 0, RoundingMode.HALF_UP);

        if (savingsRate.compareTo(BigDecimal.ZERO) >= 0) {
            insights.add("Você conseguiu guardar " + savingsRate + "% da sua renda este mês.");
        } else {
            insights.add("Suas despesas superaram a sua renda em " + savingsRate.abs() + "% este mês - vale revisar os gastos.");
        }
    }

    private void addInvestmentShareInsight(List<String> insights, Long userId) {
        BigDecimal accountsTotal = accountRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investmentsTotal = investmentRepository.sumCurrentAmountByUser(userId);

        BigDecimal patrimonio = accountsTotal.add(investmentsTotal);
        if (patrimonio.compareTo(BigDecimal.ZERO) <= 0 || investmentsTotal.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal pct = investmentsTotal.multiply(BigDecimal.valueOf(100)).divide(patrimonio, 0, RoundingMode.HALF_UP);
        insights.add("Seus investimentos representam " + pct + "% do seu patrimônio total.");
    }

    private void addProjectionInsight(List<String> insights, Long userId, BigDecimal income, BigDecimal expenses) {
        BigDecimal monthlySavings = income.subtract(expenses);
        if (monthlySavings.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal accountsTotal = accountRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investmentsTotal = investmentRepository.sumCurrentAmountByUser(userId);
        BigDecimal startingPoint = accountsTotal.add(investmentsTotal);

        BigDecimal rate = financialSettingsService.getRates(userId).fixedIncomeRate();
        var simulation = FinanceMath.simulate(startingPoint, monthlySavings, rate, 60);
        BigDecimal projected = simulation.get(simulation.size() - 1).balance();

        insights.add("Mantendo o aporte mensal atual de " + formatCurrency(monthlySavings) +
                " a uma rentabilidade estimada de " + rate + "% ao ano, sua projeção para 5 anos é de aproximadamente " +
                formatCurrency(projected) + ".");
    }

    private Map<Long, BigDecimal> groupByCategory(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) map.put((Long) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) return "sem categoria";
        return categoryRepository.findById(categoryId).map(Category::getName).orElse("essa categoria");
    }

    private String formatCurrency(BigDecimal value) {
        return java.text.NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(value);
    }
}
