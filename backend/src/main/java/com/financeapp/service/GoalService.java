package com.financeapp.service;

import com.financeapp.dto.GoalDtos.*;
import com.financeapp.entity.Goal;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.GoalRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public List<GoalResponse> list(Long userId) {
        return goalRepository.findByUserIdOrderByDeadlineAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GoalResponse create(Long userId, GoalRequest request) {
        Goal goal = Goal.builder()
                .user(userRepository.getReferenceById(userId))
                .name(request.name())
                .targetAmount(request.targetAmount())
                .currentAmount(request.currentAmount() != null ? request.currentAmount() : BigDecimal.ZERO)
                .deadline(request.deadline())
                .build();
        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse update(Long userId, Long goalId, GoalRequest request) {
        Goal goal = findOwned(userId, goalId);
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        if (request.currentAmount() != null) goal.setCurrentAmount(request.currentAmount());
        goal.setDeadline(request.deadline());
        return toResponse(goalRepository.save(goal));
    }

    /**
     * Bug corrigido: antes buscava a meta sem travar a linha no banco. Se
     * você e sua esposa contribuírem pra mesma meta quase ao mesmo tempo,
     * as duas leituras pegavam o mesmo valor "antigo" e uma das duas
     * contribuições era perdida (o clássico lost update). Agora a leitura
     * trava a linha até salvar, então a segunda contribuição só começa
     * depois que a primeira terminou de verdade.
     */
    @Transactional
    public GoalResponse contribute(Long userId, Long goalId, ContributeRequest request) {
        Goal goal = goalRepository.findByIdAndUserIdForUpdate(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada ou não pertence a você"));
        goal.setCurrentAmount(goal.getCurrentAmount().add(request.amount()));
        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public void delete(Long userId, Long goalId) {
        goalRepository.delete(findOwned(userId, goalId));
    }

    private Goal findOwned(Long userId, Long goalId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada ou não pertence a você"));
    }

    private GoalResponse toResponse(Goal g) {
        BigDecimal progress = g.getTargetAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : g.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                    .divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100));

        Long monthsRemaining = null;
        BigDecimal monthlyNeeded = null;
        if (g.getDeadline() != null) {
            long months = ChronoUnit.MONTHS.between(
                    LocalDate.now().withDayOfMonth(1), g.getDeadline().withDayOfMonth(1));
            monthsRemaining = Math.max(months, 0);
            BigDecimal remaining = g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO);
            monthlyNeeded = monthsRemaining == 0
                    ? remaining
                    : remaining.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.HALF_UP);
        }

        return new GoalResponse(
                g.getId(), g.getName(), g.getTargetAmount(), g.getCurrentAmount(), g.getDeadline(),
                progress, monthlyNeeded, monthsRemaining
        );
    }
}
