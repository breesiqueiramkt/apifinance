package com.financeapp.service;

import com.financeapp.dto.GoalDtos.ContributeRequest;
import com.financeapp.dto.GoalDtos.GoalRequest;
import com.financeapp.entity.Goal;
import com.financeapp.entity.User;
import com.financeapp.repository.GoalRepository;
import com.financeapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GoalServiceTest {

    @Mock private GoalRepository goalRepository;
    @Mock private UserRepository userRepository;

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        goalService = new GoalService(goalRepository, userRepository);
    }

    @Test
    void calculatesProgressPercentAndMonthlyContributionNeeded() {
        User user = User.builder().id(1L).build();
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate deadline = LocalDate.now().withDayOfMonth(1).plusMonths(6);
        GoalRequest request = new GoalRequest("Reserva de emergência", new BigDecimal("20000"), new BigDecimal("8000"), deadline);

        var response = goalService.create(1L, request);

        // exemplo do próprio briefing: 8.000 / 20.000 = 40%
        assertThat(response.progressPercent()).isEqualByComparingTo("40.00");
        assertThat(response.monthsRemaining()).isEqualTo(6L);
        // (20000 - 8000) / 6 meses = 2000/mês
        assertThat(response.monthlyContributionNeeded()).isEqualByComparingTo("2000.00");
    }

    @Test
    void progressNeverExceedsOneHundredPercentEvenIfOvershot() {
        User user = User.builder().id(1L).build();
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        GoalRequest request = new GoalRequest("Viagem", new BigDecimal("1000"), new BigDecimal("1500"), null);
        var response = goalService.create(1L, request);

        assertThat(response.progressPercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void contributeAddsToCurrentAmountUsingTheLockedLookup() {
        // Bug corrigido: contribute() precisa usar a busca com trava
        // (findByIdAndUserIdForUpdate), não a busca simples - senão duas
        // contribuições feitas quase ao mesmo tempo podem se sobrescrever.
        Goal goal = Goal.builder().id(9L).name("Viagem").targetAmount(new BigDecimal("1000"))
                .currentAmount(new BigDecimal("300")).build();
        when(goalRepository.findByIdAndUserIdForUpdate(9L, 1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = goalService.contribute(1L, 9L, new ContributeRequest(new BigDecimal("150")));

        assertThat(response.currentAmount()).isEqualByComparingTo("450");
    }
}
