package com.financeapp.service;

import com.financeapp.dto.ReportDtos.*;
import com.financeapp.entity.Account;
import com.financeapp.entity.TransactionStatus;
import com.financeapp.entity.TransactionType;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    private static final List<TransactionStatus> REALIZED = List.of(TransactionStatus.PAID);
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "BR"));

    public CashflowReport cashflow(Long userId, int months) {
        List<CashflowPoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate reference = today.minusMonths(i);
            LocalDate start = reference.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate end = reference.with(TemporalAdjusters.lastDayOfMonth());

            BigDecimal income = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                    userId, TransactionType.INCOME, REALIZED, start, end);
            BigDecimal expenses = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                    userId, TransactionType.EXPENSE, REALIZED, start, end);

            points.add(new CashflowPoint(reference.format(LABEL_FORMAT), income, expenses));
        }
        return new CashflowReport(points);
    }

    /**
     * Reconstrói o patrimônio (apenas contas - ver limitação abaixo) mês a mês,
     * partindo do valor atual e "andando pra trás" com os fluxos realizados de
     * cada mês. MESMA simplificação assumida no dashboard: assume que o saldo
     * só mudou por lançamentos PAID neste sistema. Investimentos não entram
     * aqui porque não há histórico de valor deles - só o valor atual é
     * conhecido (ver módulo de Investimentos).
     */
    public NetWorthReport netWorthEvolution(Long userId, int months) {
        List<Account> accounts = accountRepository.findByUserIdOrderByNameAsc(userId);
        BigDecimal currentNetWorth = accounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();
        List<BigDecimal> monthlyDeltas = new ArrayList<>(); // income - expense, do mais antigo pro mais recente
        List<String> labels = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate reference = today.minusMonths(i);
            LocalDate start = reference.with(TemporalAdjusters.firstDayOfMonth());
            LocalDate end = reference.with(TemporalAdjusters.lastDayOfMonth());

            BigDecimal income = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                    userId, TransactionType.INCOME, REALIZED, start, end);
            BigDecimal expenses = transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                    userId, TransactionType.EXPENSE, REALIZED, start, end);

            monthlyDeltas.add(income.subtract(expenses));
            labels.add(reference.format(LABEL_FORMAT));
        }

        List<NetWorthPoint> points = new ArrayList<>();
        BigDecimal runningWorth = currentNetWorth;
        // preenche de trás pra frente (do mês mais recente pro mais antigo) e depois inverte
        List<NetWorthPoint> reversed = new ArrayList<>();
        for (int idx = monthlyDeltas.size() - 1; idx >= 0; idx--) {
            reversed.add(new NetWorthPoint(labels.get(idx), runningWorth));
            runningWorth = runningWorth.subtract(monthlyDeltas.get(idx));
        }
        for (int idx = reversed.size() - 1; idx >= 0; idx--) {
            points.add(reversed.get(idx));
        }

        return new NetWorthReport(points);
    }

    public CategoryReport expensesByCategory(Long userId, LocalDate start, LocalDate end) {
        List<Object[]> rows = transactionRepository.sumGroupedByCategory(userId, TransactionType.EXPENSE, start, end);

        List<CategorySlice> slices = rows.stream().map(row -> {
            Long categoryId = (Long) row[0];
            BigDecimal total = (BigDecimal) row[1];
            if (categoryId == null) return new CategorySlice(null, "Sem categoria", "📦", total);
            return categoryRepository.findById(categoryId)
                    .map(c -> new CategorySlice(c.getId(), c.getName(), c.getIcon(), total))
                    .orElse(new CategorySlice(categoryId, "Categoria removida", "📦", total));
        }).sorted((a, b) -> b.total().compareTo(a.total())).toList();

        BigDecimal total = slices.stream().map(CategorySlice::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CategoryReport(slices, total);
    }
}
