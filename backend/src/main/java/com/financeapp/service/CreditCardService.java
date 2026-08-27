package com.financeapp.service;

import com.financeapp.dto.CreditCardDtos.*;
import com.financeapp.entity.Category;
import com.financeapp.entity.CreditCard;
import com.financeapp.entity.CreditCardTransaction;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.CreditCardRepository;
import com.financeapp.repository.CreditCardTransactionRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Modelo de fatura: cada parcela de uma compra é gravada com a purchase_date
 * já projetada para o mês em que ela vai cobrar (compra + N meses). Assim,
 * "fatura de um período" é sempre só um filtro por intervalo de datas -
 * ver método currentCycle()/nextCycle().
 */
@Service
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardTransactionRepository ccTransactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<CreditCardResponse> list(Long userId) {
        return creditCardRepository.findByUserIdOrderByNameAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CreditCardResponse create(Long userId, CreditCardRequest request) {
        CreditCard card = CreditCard.builder()
                .user(userRepository.getReferenceById(userId))
                .name(request.name())
                .bank(request.bank())
                .creditLimit(request.creditLimit())
                .closingDay(request.closingDay())
                .dueDay(request.dueDay())
                .build();
        return toResponse(creditCardRepository.save(card));
    }

    @Transactional
    public void delete(Long userId, Long cardId) {
        creditCardRepository.delete(findOwned(userId, cardId));
    }

    public List<CreditCardTransactionResponse> listPurchases(Long userId, Long cardId) {
        findOwned(userId, cardId);
        return ccTransactionRepository.findByCreditCardIdOrderByPurchaseDateDesc(cardId)
                .stream().map(this::toTxResponse).toList();
    }

    @Transactional
    public List<CreditCardTransactionResponse> addPurchase(Long userId, Long cardId, PurchaseRequest request) {
        CreditCard card = findOwned(userId, cardId);
        Category category = resolveCategory(userId, request.categoryId());

        int n = request.installments();
        BigDecimal base = request.amount().divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal allocated = base.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = request.amount().subtract(allocated); // ajusta arredondamento na última parcela

        List<CreditCardTransaction> saved = new java.util.ArrayList<>();
        for (short i = 1; i <= n; i++) {
            BigDecimal installmentAmount = (i == n) ? base.add(remainder) : base;
            CreditCardTransaction tx = CreditCardTransaction.builder()
                    .creditCard(card)
                    .category(category)
                    .description(n > 1 ? request.description() + " (" + i + "/" + n + ")" : request.description())
                    .amount(installmentAmount)
                    .purchaseDate(request.purchaseDate().plusMonths(i - 1))
                    .installments((short) n)
                    .installmentNo(i)
                    .build();
            saved.add(ccTransactionRepository.save(tx));
        }
        return saved.stream().map(this::toTxResponse).toList();
    }

    private CreditCard findOwned(Long userId, Long cardId) {
        return creditCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado ou não pertence a você"));
    }

    private Category resolveCategory(Long userId, Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getUser() == null || c.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada ou não pertence a você"));
    }

    // ---------- cálculo de ciclo de fatura ----------

    private LocalDate closingDateFor(YearMonth ym, short closingDay) {
        int day = Math.min(closingDay, ym.lengthOfMonth());
        return ym.atDay(day);
    }

    record CyclePeriod(LocalDate start, LocalDate end) {}

    private CyclePeriod currentCycle(short closingDay) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        LocalDate thisClose = closingDateFor(thisMonth, closingDay);

        LocalDate end = today.isAfter(thisClose) ? closingDateFor(thisMonth.plusMonths(1), closingDay) : thisClose;
        LocalDate start = closingDateFor(YearMonth.from(end).minusMonths(1), closingDay).plusDays(1);
        return new CyclePeriod(start, end);
    }

    private CyclePeriod nextCycle(CyclePeriod current, short closingDay) {
        LocalDate start = current.end().plusDays(1);
        LocalDate end = closingDateFor(YearMonth.from(current.end()).plusMonths(1), closingDay);
        return new CyclePeriod(start, end);
    }

    private CreditCardResponse toResponse(CreditCard card) {
        CyclePeriod current = currentCycle(card.getClosingDay());
        CyclePeriod next = nextCycle(current, card.getClosingDay());

        BigDecimal currentInvoice = ccTransactionRepository.sumByCardAndPeriod(card.getId(), current.start(), current.end());
        BigDecimal nextInvoice = ccTransactionRepository.sumByCardAndPeriod(card.getId(), next.start(), next.end());
        BigDecimal limitUsed = ccTransactionRepository.sumOutstandingFromDate(card.getId(), current.start());
        BigDecimal limitAvailable = card.getCreditLimit().subtract(limitUsed);

        return new CreditCardResponse(
                card.getId(), card.getName(), card.getBank(), card.getCreditLimit(),
                card.getClosingDay(), card.getDueDay(),
                currentInvoice, nextInvoice, limitUsed, limitAvailable
        );
    }

    private CreditCardTransactionResponse toTxResponse(CreditCardTransaction t) {
        return new CreditCardTransactionResponse(
                t.getId(), t.getDescription(), t.getAmount(), t.getPurchaseDate(),
                t.getInstallments(), t.getInstallmentNo(),
                t.getCategory() != null ? t.getCategory().getName() : null
        );
    }
}
