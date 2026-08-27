package com.financeapp.service;

import com.financeapp.dto.BillDtos.BillRequest;
import com.financeapp.dto.BillDtos.BillResponse;
import com.financeapp.dto.TransactionDtos.TransactionRequest;
import com.financeapp.entity.*;
import com.financeapp.exception.BusinessException;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.BillRepository;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public List<BillResponse> list(Long userId, Integer nextDays) {
        List<Bill> bills = (nextDays != null)
                ? billRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                        userId, LocalDate.now(), LocalDate.now().plusDays(nextDays))
                : billRepository.findByUserIdOrderByDueDateAsc(userId);
        return bills.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BillResponse create(Long userId, BillRequest request) {
        Bill bill = Bill.builder()
                .user(userRepository.getReferenceById(userId))
                .account(resolveAccount(userId, request.accountId()))
                .category(resolveCategory(userId, request.categoryId()))
                .description(request.description())
                .amount(request.amount())
                .dueDate(request.dueDate())
                .recurrence(request.recurrence() != null ? request.recurrence() : RecurrenceType.NONE)
                .status(BillStatus.PENDING)
                .build();
        return toResponse(billRepository.save(bill));
    }

    @Transactional
    public BillResponse update(Long userId, Long billId, BillRequest request) {
        Bill bill = findOwned(userId, billId);
        bill.setAccount(resolveAccount(userId, request.accountId()));
        bill.setCategory(resolveCategory(userId, request.categoryId()));
        bill.setDescription(request.description());
        bill.setAmount(request.amount());
        bill.setDueDate(request.dueDate());
        bill.setRecurrence(request.recurrence() != null ? request.recurrence() : bill.getRecurrence());
        return toResponse(billRepository.save(bill));
    }

    @Transactional
    public void delete(Long userId, Long billId) {
        billRepository.delete(findOwned(userId, billId));
    }

    /**
     * Marca a conta como paga: cria um lançamento de despesa PAID de verdade
     * (que já atualiza o saldo da conta via TransactionService) e muda o
     * status do boleto para PAID. Isso integra "contas futuras" ao extrato
     * real em vez de ser só um checklist solto.
     *
     * Bug corrigido: antes não havia checagem de status, então clicar em
     * "pagar" duas vezes (ex: duplo clique, ou você e sua esposa confirmando
     * quase ao mesmo tempo) criava DOIS lançamentos de despesa e descontava
     * o valor da conta duas vezes. Agora a segunda tentativa é rejeitada.
     */
    @Transactional
    public BillResponse markAsPaid(Long userId, Long billId, Long accountIdOverride) {
        Bill bill = findOwned(userId, billId);

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException("Esta conta já foi paga - ela não pode ser paga de novo.");
        }

        Long accountId = accountIdOverride != null
                ? accountIdOverride
                : (bill.getAccount() != null ? bill.getAccount().getId() : null);

        if (accountId == null) {
            throw new BusinessException("Informe a conta que será usada para pagar esta conta.");
        }

        TransactionRequest txRequest = new TransactionRequest(
                accountId,
                bill.getCategory() != null ? bill.getCategory().getId() : null,
                TransactionType.EXPENSE,
                bill.getDescription(),
                bill.getAmount(),
                LocalDate.now(),
                null,
                TransactionStatus.PAID,
                RecurrenceType.NONE,
                "Pago via módulo de Contas Futuras"
        );
        transactionService.create(userId, txRequest);

        bill.setStatus(BillStatus.PAID);
        BillResponse response = toResponse(billRepository.save(bill));

        // Bug corrigido: a conta tinha um campo de recorrência (mensal/semanal/
        // anual) que nunca gerava a próxima ocorrência - depois de paga, a
        // conta recorrente simplesmente sumia da lista em vez de voltar a
        // aparecer no próximo período. Agora, ao pagar uma conta recorrente,
        // a próxima já é criada automaticamente como pendente.
        if (bill.getRecurrence() != RecurrenceType.NONE) {
            createNextOccurrence(bill);
        }

        return response;
    }

    private void createNextOccurrence(Bill paidBill) {
        LocalDate nextDueDate = switch (paidBill.getRecurrence()) {
            case WEEKLY -> paidBill.getDueDate().plusWeeks(1);
            case MONTHLY -> paidBill.getDueDate().plusMonths(1);
            case YEARLY -> paidBill.getDueDate().plusYears(1);
            case NONE -> null;
        };
        if (nextDueDate == null) return;

        Bill next = Bill.builder()
                .user(paidBill.getUser())
                .account(paidBill.getAccount())
                .category(paidBill.getCategory())
                .description(paidBill.getDescription())
                .amount(paidBill.getAmount())
                .dueDate(nextDueDate)
                .recurrence(paidBill.getRecurrence())
                .status(BillStatus.PENDING)
                .build();
        billRepository.save(next);
    }

    private Account resolveAccount(Long userId, Long accountId) {
        if (accountId == null) return null;
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada ou não pertence a você"));
    }

    private Category resolveCategory(Long userId, Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getUser() == null || c.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada ou não pertence a você"));
    }

    private Bill findOwned(Long userId, Long billId) {
        return billRepository.findByIdAndUserId(billId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada ou não pertence a você"));
    }

    private BillResponse toResponse(Bill b) {
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), b.getDueDate());
        return new BillResponse(
                b.getId(), b.getDescription(), b.getAmount(), b.getDueDate(),
                b.getAccount() != null ? b.getAccount().getId() : null,
                b.getAccount() != null ? b.getAccount().getName() : null,
                b.getCategory() != null ? b.getCategory().getId() : null,
                b.getCategory() != null ? b.getCategory().getName() : null,
                b.getRecurrence(), b.getStatus(), daysUntilDue
        );
    }
}
