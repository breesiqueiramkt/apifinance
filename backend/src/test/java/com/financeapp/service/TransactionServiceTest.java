package com.financeapp.service;

import com.financeapp.dto.TransactionDtos.TransactionRequest;
import com.financeapp.entity.*;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.TransactionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    private TransactionService transactionService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionService(
                transactionRepository, accountRepository, categoryRepository, userRepository);

        user = User.builder().id(1L).name("Breno").email("breno@example.com").build();
        account = Account.builder()
                .id(10L).user(user).name("Nubank").type(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00")).build();

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        // create/update/delete buscam a conta já travada (SELECT ... FOR UPDATE) para
        // evitar lost updates quando duas pessoas lançam algo quase ao mesmo tempo -
        // ver AccountRepository.findByIdAndUserIdForUpdate
        when(accountRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(account));
        // devolve a própria entidade recebida, como faria um save() real
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void paidIncomeTransactionIncreasesAccountBalance() {
        TransactionRequest request = new TransactionRequest(
                10L, null, TransactionType.INCOME, "Salário", new BigDecimal("2000.00"),
                LocalDate.now(), null, TransactionStatus.PAID, RecurrenceType.NONE, null);

        transactionService.create(1L, request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("3000.00");
    }

    @Test
    void paidExpenseTransactionDecreasesAccountBalance() {
        TransactionRequest request = new TransactionRequest(
                10L, null, TransactionType.EXPENSE, "Aluguel", new BigDecimal("700.00"),
                LocalDate.now(), null, TransactionStatus.PAID, RecurrenceType.NONE, null);

        transactionService.create(1L, request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    void pendingTransactionDoesNotTouchAccountBalance() {
        TransactionRequest request = new TransactionRequest(
                10L, null, TransactionType.EXPENSE, "Internet", new BigDecimal("120.00"),
                LocalDate.now().plusDays(5), null, TransactionStatus.PENDING, RecurrenceType.NONE, null);

        transactionService.create(1L, request);

        // saldo não deve ser tocado enquanto o lançamento estiver pendente
        verify(accountRepository, never()).save(any());
        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void deletingPaidTransactionReversesItsEffectOnBalance() {
        Transaction paid = Transaction.builder()
                .id(99L).user(user).account(account).type(TransactionType.EXPENSE)
                .description("Mercado").amount(new BigDecimal("150.00"))
                .date(LocalDate.now()).status(TransactionStatus.PAID)
                .recurrence(RecurrenceType.NONE).build();

        account.setBalance(new BigDecimal("850.00")); // já refletindo a despesa acima
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(paid));

        transactionService.delete(1L, 99L);

        assertThat(account.getBalance()).isEqualByComparingTo("1000.00");
        verify(transactionRepository).delete(paid);
    }
}
