package com.financeapp.service;

import com.financeapp.entity.Bill;
import com.financeapp.entity.BillStatus;
import com.financeapp.entity.RecurrenceType;
import com.financeapp.entity.User;
import com.financeapp.exception.BusinessException;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.BillRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private TransactionService transactionService;

    private BillService billService;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        billService = new BillService(billRepository, accountRepository, categoryRepository, userRepository, transactionService);
        user = User.builder().id(1L).name("Família").build();
    }

    @Test
    void payingABillAlreadyPaidIsRejected() {
        Bill bill = Bill.builder()
                .id(5L).user(user).description("Internet").amount(new BigDecimal("120.00"))
                .dueDate(LocalDate.now()).recurrence(RecurrenceType.NONE).status(BillStatus.PAID)
                .build();
        when(billRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> billService.markAsPaid(1L, 5L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já foi paga");

        // não pode ter criado um lançamento de despesa nem descontado a conta de novo
        verify(transactionService, never()).create(any(), any());
    }

    @Test
    void payingAMonthlyRecurringBillCreatesTheNextOccurrencePending() {
        LocalDate dueDate = LocalDate.of(2026, 3, 10);
        Bill bill = Bill.builder()
                .id(7L).user(user).description("Aluguel").amount(new BigDecimal("1500.00"))
                .dueDate(dueDate).recurrence(RecurrenceType.MONTHLY).status(BillStatus.PENDING)
                .build();
        when(billRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        billService.markAsPaid(1L, 7L, 10L);

        ArgumentCaptor<Bill> captor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository, times(2)).save(captor.capture());

        Bill next = captor.getAllValues().get(1);
        assertThat(next.getStatus()).isEqualTo(BillStatus.PENDING);
        assertThat(next.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(next.getRecurrence()).isEqualTo(RecurrenceType.MONTHLY);
        assertThat(next.getAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void payingANonRecurringBillDoesNotCreateAnyFollowUp() {
        Bill bill = Bill.builder()
                .id(9L).user(user).description("Presente").amount(new BigDecimal("80.00"))
                .dueDate(LocalDate.now()).recurrence(RecurrenceType.NONE).status(BillStatus.PENDING)
                .build();
        when(billRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        billService.markAsPaid(1L, 9L, 10L);

        verify(billRepository, times(1)).save(any(Bill.class));
    }
}
