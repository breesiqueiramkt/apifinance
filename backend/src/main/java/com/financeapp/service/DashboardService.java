package com.financeapp.service;

import com.financeapp.dto.DashboardDtos.CategoryBreakdown;
import com.financeapp.dto.DashboardDtos.DashboardResponse;
import com.financeapp.dto.DashboardDtos.UpcomingBill;
import com.financeapp.entity.Account;
import com.financeapp.entity.Transaction;
import com.financeapp.entity.TransactionStatus;
import com.financeapp.entity.TransactionType;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.InvestmentRepository;
import com.financeapp.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final InvestmentRepository investmentRepository;

    private static final List<TransactionStatus> REALIZED = List.of(TransactionStatus.PAID);
    private static final List<TransactionStatus> INCOME_PENDING =
            List.of(TransactionStatus.PENDING, TransactionStatus.SCHEDULED);
    private static final List<TransactionStatus> EXPENSE_PENDING =
            List.of(TransactionStatus.PENDING, TransactionStatus.SCHEDULED, TransactionStatus.LATE);

    public DashboardResponse build(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        List<Account> accounts = accountRepository.findByUserIdOrderByNameAsc(userId);
        BigDecimal accountsTotal = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal investedTotal = investmentRepository.sumCurrentAmountByUser(userId);

        // "Disponível" = dinheiro líquido nas contas. "Patrimônio total" soma os investimentos.
        BigDecimal available = accountsTotal;
        BigDecimal netWorth = accountsTotal.add(investedTotal);

        BigDecimal monthlyIncome = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                userId, TransactionType.INCOME, REALIZED, monthStart, monthEnd);
        BigDecimal monthlyExpenses = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                userId, TransactionType.EXPENSE, REALIZED, monthStart, monthEnd);

        // Reconstrói o patrimônio do início do mês a partir do que já foi realizado.
        // Simplificações assumidas: (1) o saldo das contas só muda através de
        // lançamentos PAID neste módulo; (2) o valor investido é assumido igual
        // ao do mês anterior, já que não há histórico de valor de investimentos
        // ainda (só o valor atual é conhecido - ver módulo de Investimentos).
        // Uma tabela de snapshot mensal é a evolução natural para isto ficar
        // exato mesmo com edições manuais de saldo/valor investido.
        BigDecimal previousMonthAccounts = accountsTotal.subtract(monthlyIncome).add(monthlyExpenses);
        BigDecimal previousMonthNetWorth = previousMonthAccounts.add(investedTotal);

        BigDecimal pendingIncome = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                userId, TransactionType.INCOME, INCOME_PENDING, monthStart, monthEnd);
        BigDecimal pendingExpense = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                userId, TransactionType.EXPENSE, EXPENSE_PENDING, monthStart, monthEnd);
        BigDecimal projectedBalance = available.add(pendingIncome).subtract(pendingExpense);

        BigDecimal savingsRate = percentage(monthlyIncome.subtract(monthlyExpenses), monthlyIncome);
        BigDecimal expenseCommitment = percentage(monthlyExpenses, monthlyIncome);

        List<CategoryBreakdown> expensesByCategory = buildCategoryBreakdown(userId, monthStart, monthEnd);

        List<UpcomingBill> upcoming = transactionRepository
                .findByUserIdAndStatusInAndDateBetweenOrderByDateAsc(
                        userId, EXPENSE_PENDING, today, today.plusDays(30))
                .stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(this::toUpcomingBill)
                .toList();

        return new DashboardResponse(
                netWorth, available, investedTotal, monthlyIncome, monthlyExpenses,
                previousMonthNetWorth, projectedBalance, savingsRate, expenseCommitment,
                expensesByCategory, upcoming
        );
    }

    private List<CategoryBreakdown> buildCategoryBreakdown(Long userId, LocalDate start, LocalDate end) {
        List<Object[]> rows = transactionRepository.sumGroupedByCategory(userId, TransactionType.EXPENSE, start, end);
        return rows.stream().map(row -> {
            Long categoryId = (Long) row[0];
            BigDecimal total = (BigDecimal) row[1];
            if (categoryId == null) {
                return new CategoryBreakdown(null, "Sem categoria", "📦", total);
            }
            return categoryRepository.findById(categoryId)
                    .map(c -> new CategoryBreakdown(c.getId(), c.getName(), c.getIcon(), total))
                    .orElse(new CategoryBreakdown(categoryId, "Categoria removida", "📦", total));
        }).sorted((a, b) -> b.total().compareTo(a.total())).collect(Collectors.toList());
    }

    private UpcomingBill toUpcomingBill(Transaction t) {
        return new UpcomingBill(t.getId(), t.getDescription(), t.getAmount(),
                t.getDate().format(DateTimeFormatter.ISO_DATE));
    }

    /** retorna 0 quando a base (receita) é zero, para nunca dividir por zero */
    private BigDecimal percentage(BigDecimal numerator, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return numerator.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
}
