package com.financeapp.service;

import com.financeapp.dto.InvestmentDtos.*;
import com.financeapp.entity.Investment;
import com.financeapp.entity.InvestmentClass;
import com.financeapp.entity.InvestmentType;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.InvestmentRepository;
import com.financeapp.repository.InvestmentTypeRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final InvestmentTypeRepository investmentTypeRepository;
    private final UserRepository userRepository;

    private static final List<Object[]> DEFAULT_TYPES = List.of(
            new Object[]{"CDB", InvestmentClass.FIXED_INCOME},
            new Object[]{"LCI", InvestmentClass.FIXED_INCOME},
            new Object[]{"LCA", InvestmentClass.FIXED_INCOME},
            new Object[]{"Tesouro Direto", InvestmentClass.FIXED_INCOME},
            new Object[]{"Poupança", InvestmentClass.FIXED_INCOME},
            new Object[]{"Ações", InvestmentClass.VARIABLE_INCOME},
            new Object[]{"FIIs", InvestmentClass.VARIABLE_INCOME},
            new Object[]{"ETFs", InvestmentClass.VARIABLE_INCOME},
            new Object[]{"Criptomoedas", InvestmentClass.OTHER},
            new Object[]{"Fundos", InvestmentClass.OTHER},
            new Object[]{"Outros", InvestmentClass.OTHER}
    );

    @Transactional
    public void ensureDefaultTypesExist() {
        for (Object[] def : DEFAULT_TYPES) {
            String name = (String) def[0];
            if (investmentTypeRepository.findByNameIgnoreCase(name).isEmpty()) {
                investmentTypeRepository.save(InvestmentType.builder()
                        .name(name)
                        .investmentClass((InvestmentClass) def[1])
                        .build());
            }
        }
    }

    public List<InvestmentTypeResponse> listTypes() {
        ensureDefaultTypesExist();
        return investmentTypeRepository.findAllByOrderByNameAsc().stream()
                .map(t -> new InvestmentTypeResponse(t.getId(), t.getName(), t.getInvestmentClass().name()))
                .toList();
    }

    public List<InvestmentResponse> list(Long userId) {
        return investmentRepository.findByUserIdOrderByInvestedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public InvestmentResponse create(Long userId, InvestmentRequest request) {
        Investment investment = Investment.builder()
                .user(userRepository.getReferenceById(userId))
                .investmentType(resolveType(request.investmentTypeId()))
                .name(request.name())
                .investedAmount(request.investedAmount())
                .currentAmount(request.currentAmount())
                .investedAt(request.investedAt())
                .expectedRate(request.expectedRate())
                .institution(request.institution())
                .notes(request.notes())
                .build();
        return toResponse(investmentRepository.save(investment));
    }

    @Transactional
    public InvestmentResponse update(Long userId, Long investmentId, InvestmentRequest request) {
        Investment investment = findOwned(userId, investmentId);
        investment.setInvestmentType(resolveType(request.investmentTypeId()));
        investment.setName(request.name());
        investment.setInvestedAmount(request.investedAmount());
        investment.setCurrentAmount(request.currentAmount());
        investment.setInvestedAt(request.investedAt());
        investment.setExpectedRate(request.expectedRate());
        investment.setInstitution(request.institution());
        investment.setNotes(request.notes());
        return toResponse(investmentRepository.save(investment));
    }

    @Transactional
    public void delete(Long userId, Long investmentId) {
        investmentRepository.delete(findOwned(userId, investmentId));
    }

    public InvestmentSummary summary(Long userId) {
        List<Investment> investments = investmentRepository.findByUserIdOrderByInvestedAtDesc(userId);

        BigDecimal totalInvested = investmentRepository.sumInvestedAmountByUser(userId);
        BigDecimal totalCurrent = investmentRepository.sumCurrentAmountByUser(userId);
        BigDecimal totalReturn = totalCurrent.subtract(totalInvested);
        BigDecimal totalReturnPercent = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalReturn.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP);

        // renda estimada = soma de (valor atual de cada investimento × sua taxa esperada / 12)
        BigDecimal estimatedMonthlyIncome = investments.stream()
                .filter(i -> i.getExpectedRate() != null)
                .map(i -> i.getCurrentAmount()
                        .multiply(i.getExpectedRate())
                        .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedYearlyIncome = estimatedMonthlyIncome.multiply(BigDecimal.valueOf(12));

        return new InvestmentSummary(
                totalInvested, totalCurrent, totalReturn, totalReturnPercent,
                estimatedMonthlyIncome.setScale(2, RoundingMode.HALF_UP),
                estimatedYearlyIncome.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private InvestmentType resolveType(Long typeId) {
        if (typeId == null) return null;
        return investmentTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de investimento não encontrado"));
    }

    private Investment findOwned(Long userId, Long investmentId) {
        return investmentRepository.findByIdAndUserId(investmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado ou não pertence a você"));
    }

    private InvestmentResponse toResponse(Investment i) {
        BigDecimal returnAmount = i.getCurrentAmount().subtract(i.getInvestedAmount());
        BigDecimal returnPercent = i.getInvestedAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : returnAmount.multiply(BigDecimal.valueOf(100)).divide(i.getInvestedAmount(), 2, RoundingMode.HALF_UP);

        return new InvestmentResponse(
                i.getId(), i.getName(),
                i.getInvestmentType() != null ? i.getInvestmentType().getId() : null,
                i.getInvestmentType() != null ? i.getInvestmentType().getName() : null,
                i.getInvestedAmount(), i.getCurrentAmount(), returnAmount, returnPercent,
                i.getInvestedAt(), i.getExpectedRate(), i.getInstitution(), i.getNotes()
        );
    }
}
