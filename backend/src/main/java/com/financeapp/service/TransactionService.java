package com.financeapp.service;

import com.financeapp.dto.TransactionDtos.TransactionRequest;
import com.financeapp.dto.TransactionDtos.TransactionResponse;
import com.financeapp.entity.*;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Regra central: o saldo de uma conta só é afetado por lançamentos com
 * status PAID (dinheiro que já entrou/saiu de fato). Lançamentos PENDING,
 * SCHEDULED ou LATE aparecem em "contas futuras" mas não mexem no saldo
 * até serem marcados como pagos - assim o saldo disponível reflete a
 * realidade da conta bancária, e a "previsão" do dashboard usa os
 * pendentes separadamente (ver DashboardService).
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<TransactionResponse> list(Long userId, LocalDate start, LocalDate end) {
        List<Transaction> transactions = (start != null && end != null)
                ? transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end)
                : transactionRepository.findByUserIdOrderByDateDesc(userId);
        return transactions.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        Account account = accountRepository.findByIdAndUserIdForUpdate(request.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada ou não pertence a você"));

        Category category = resolveCategory(userId, request.categoryId());

        Transaction transaction = Transaction.builder()
                .user(userRepository.getReferenceById(userId))
                .account(account)
                .category(category)
                .type(request.type())
                .description(request.description())
                .amount(request.amount())
                .date(request.date())
                .paymentMethod(request.paymentMethod())
                .status(request.status() != null ? request.status() : TransactionStatus.PAID)
                .recurrence(request.recurrence() != null ? request.recurrence() : RecurrenceType.NONE)
                .notes(request.notes())
                .build();

        if (transaction.getStatus() == TransactionStatus.PAID) {
            applyToBalance(account, transaction.getType(), transaction.getAmount());
            accountRepository.save(account);
        }

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado ou não pertence a você"));

        // reverte o efeito anterior no saldo (se havia) - busca a conta antiga já travada (ver findByIdAndUserIdForUpdate)
        if (transaction.getStatus() == TransactionStatus.PAID) {
            Account oldAccount = accountRepository.findByIdAndUserIdForUpdate(transaction.getAccount().getId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada ou não pertence a você"));
            reverseFromBalance(oldAccount, transaction.getType(), transaction.getAmount());
            accountRepository.save(oldAccount);
        }

        Account newAccount = accountRepository.findByIdAndUserIdForUpdate(request.accountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada ou não pertence a você"));
        Category category = resolveCategory(userId, request.categoryId());

        transaction.setAccount(newAccount);
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setStatus(request.status() != null ? request.status() : transaction.getStatus());
        transaction.setRecurrence(request.recurrence() != null ? request.recurrence() : transaction.getRecurrence());
        transaction.setNotes(request.notes());

        // aplica o novo efeito no saldo (se for pago)
        if (transaction.getStatus() == TransactionStatus.PAID) {
            applyToBalance(newAccount, transaction.getType(), transaction.getAmount());
            accountRepository.save(newAccount);
        }

        return toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado ou não pertence a você"));

        if (transaction.getStatus() == TransactionStatus.PAID) {
            Account account = accountRepository.findByIdAndUserIdForUpdate(transaction.getAccount().getId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada ou não pertence a você"));
            reverseFromBalance(account, transaction.getType(), transaction.getAmount());
            accountRepository.save(account);
        }

        transactionRepository.delete(transaction);
    }

    private Category resolveCategory(Long userId, Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getUser() == null || c.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada ou não pertence a você"));
    }

    private void applyToBalance(Account account, TransactionType type, BigDecimal amount) {
        account.setBalance(type == TransactionType.INCOME
                ? account.getBalance().add(amount)
                : account.getBalance().subtract(amount));
    }

    private void reverseFromBalance(Account account, TransactionType type, BigDecimal amount) {
        account.setBalance(type == TransactionType.INCOME
                ? account.getBalance().subtract(amount)
                : account.getBalance().add(amount));
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAccount().getId(),
                t.getAccount().getName(),
                t.getCategory() != null ? t.getCategory().getId() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getType(),
                t.getDescription(),
                t.getAmount(),
                t.getDate(),
                t.getPaymentMethod(),
                t.getStatus(),
                t.getRecurrence(),
                t.getNotes()
        );
    }
}
