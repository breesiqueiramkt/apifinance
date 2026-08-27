package com.financeapp.service;

import com.financeapp.dto.DebtDtos.*;
import com.financeapp.entity.Debt;
import com.financeapp.entity.DebtStatus;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.DebtRepository;
import com.financeapp.repository.UserRepository;
import com.financeapp.util.FinanceMath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final UserRepository userRepository;

    /** Ordenada por taxa de juros decrescente = método "avalanche": quita primeiro quem mais encarece. */
    public List<DebtResponse> list(Long userId) {
        return debtRepository.findByUserIdOrderByInterestRateDescCurrentAmountDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DebtResponse create(Long userId, DebtRequest request) {
        Debt debt = Debt.builder()
                .user(userRepository.getReferenceById(userId))
                .creditor(request.creditor())
                .originalAmount(request.originalAmount())
                .currentAmount(request.currentAmount())
                .interestRate(request.interestRate())
                .installmentsTotal(request.installmentsTotal())
                .installmentsPaid(request.installmentsPaid() != null ? request.installmentsPaid() : 0)
                .dueDate(request.dueDate())
                .status(request.status() != null ? request.status() : DebtStatus.OPEN)
                .build();
        return toResponse(debtRepository.save(debt));
    }

    @Transactional
    public DebtResponse update(Long userId, Long debtId, DebtRequest request) {
        Debt debt = findOwned(userId, debtId);
        debt.setCreditor(request.creditor());
        debt.setOriginalAmount(request.originalAmount());
        debt.setCurrentAmount(request.currentAmount());
        debt.setInterestRate(request.interestRate());
        debt.setInstallmentsTotal(request.installmentsTotal());
        if (request.installmentsPaid() != null) debt.setInstallmentsPaid(request.installmentsPaid());
        debt.setDueDate(request.dueDate());
        if (request.status() != null) debt.setStatus(request.status());
        return toResponse(debtRepository.save(debt));
    }

    @Transactional
    public void delete(Long userId, Long debtId) {
        debtRepository.delete(findOwned(userId, debtId));
    }

    public DebtSummary summary(Long userId) {
        List<Debt> debts = debtRepository.findByUserIdOrderByInterestRateDescCurrentAmountDesc(userId);
        BigDecimal totalOriginal = debts.stream().map(Debt::getOriginalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRemaining = debts.stream()
                .filter(d -> d.getStatus() == DebtStatus.OPEN)
                .map(Debt::getCurrentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = totalOriginal.subtract(totalRemaining);
        return new DebtSummary(totalOriginal, totalPaid, totalRemaining);
    }

    private Debt findOwned(Long userId, Long debtId) {
        return debtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Dívida não encontrada ou não pertence a você"));
    }

    private DebtResponse toResponse(Debt d) {
        BigDecimal paidAmount = d.getOriginalAmount().subtract(d.getCurrentAmount());

        BigDecimal estimatedInstallmentValue = null;
        BigDecimal estimatedRemainingInterest = null;

        if (d.getInterestRate() != null && d.getInstallmentsTotal() != null) {
            int remaining = d.getInstallmentsTotal() - d.getInstallmentsPaid();
            if (remaining > 0) {
                BigDecimal monthlyRate = d.getInterestRate().divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP);
                estimatedInstallmentValue = FinanceMath.pricePayment(d.getCurrentAmount(), monthlyRate, remaining);
                BigDecimal totalToPay = estimatedInstallmentValue.multiply(BigDecimal.valueOf(remaining));
                estimatedRemainingInterest = FinanceMath.round(totalToPay.subtract(d.getCurrentAmount()));
            }
        }

        return new DebtResponse(
                d.getId(), d.getCreditor(), d.getOriginalAmount(), d.getCurrentAmount(), paidAmount,
                d.getInterestRate(), d.getInstallmentsTotal(), d.getInstallmentsPaid(), d.getDueDate(), d.getStatus(),
                estimatedInstallmentValue, estimatedRemainingInterest
        );
    }
}
