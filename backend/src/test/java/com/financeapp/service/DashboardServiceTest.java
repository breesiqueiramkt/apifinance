package com.financeapp.service;

import com.financeapp.entity.*;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.InvestmentRepository;
import com.financeapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InvestmentRepository investmentRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardService = new DashboardService(accountRepository, transactionRepository, categoryRepository, investmentRepository);
    }

    @Test
    void computesSavingsRateAndExpenseCommitmentCorrectly() {
        User user = User.builder().id(1L).build();
        Account checking = Account.builder().id(1L).user(user).balance(new BigDecimal("5500.00")).build();

        when(accountRepository.findByUserIdOrderByNameAsc(1L)).thenReturn(List.of(checking));
        when(investmentRepository.sumCurrentAmountByUser(1L)).thenReturn(BigDecimal.ZERO);

        // receita do mês: 4500,00 | despesas do mês: 2850,00  -> economia de 36,67%
        when(transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                eq(1L), eq(TransactionType.INCOME), eq(List.of(TransactionStatus.PAID)), any(), any()))
                .thenReturn(new BigDecimal("4500.00"));
        when(transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                eq(1L), eq(TransactionType.EXPENSE), eq(List.of(TransactionStatus.PAID)), any(), any()))
                .thenReturn(new BigDecimal("2850.00"));
        when(transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                eq(1L), eq(TransactionType.INCOME),
                eq(List.of(TransactionStatus.PENDING, TransactionStatus.SCHEDULED)), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                eq(1L), eq(TransactionType.EXPENSE),
                eq(List.of(TransactionStatus.PENDING, TransactionStatus.SCHEDULED, TransactionStatus.LATE)),
                any(), any()))
                .thenReturn(new BigDecimal("300.00"));
        when(transactionRepository.sumGroupedByCategory(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.findByUserIdAndStatusInAndDateBetweenOrderByDateAsc(
                eq(1L), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        var result = dashboardService.build(1L);

        assertThat(result.netWorth()).isEqualByComparingTo("5500.00");
        assertThat(result.savingsRate()).isEqualByComparingTo("36.67");   // (4500-2850)/4500*100
        assertThat(result.expenseCommitment()).isEqualByComparingTo("63.33"); // 2850/4500*100
        // saldo previsto = disponível (5500) + receita pendente (0) - despesa pendente (300)
        assertThat(result.projectedBalance()).isEqualByComparingTo("5200.00");
    }

    @Test
    void savingsRateIsZeroWhenThereIsNoIncomeToAvoidDivisionByZero() {
        when(accountRepository.findByUserIdOrderByNameAsc(1L)).thenReturn(List.of());
        when(investmentRepository.sumCurrentAmountByUser(1L)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByUserAndTypeAndStatusesAndPeriod(
                eq(1L), any(), anyList(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumGroupedByCategory(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.findByUserIdAndStatusInAndDateBetweenOrderByDateAsc(
                eq(1L), anyList(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        var result = dashboardService.build(1L);

        assertThat(result.savingsRate()).isEqualByComparingTo("0");
        assertThat(result.expenseCommitment()).isEqualByComparingTo("0");
    }
}
