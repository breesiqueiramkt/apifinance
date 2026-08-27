package com.financeapp.service;

import com.financeapp.dto.FinancialSettingsDtos.RatesRequest;
import com.financeapp.dto.FinancialSettingsDtos.RatesResponse;
import com.financeapp.entity.FinancialSettings;
import com.financeapp.repository.FinancialSettingsRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seção 10 do briefing: as taxas usadas nas calculadoras e nas projeções de
 * investimento são configuráveis, nunca fixas no código, e sempre exibidas
 * como estimativa - nunca como promessa de rendimento.
 */
@Service
@RequiredArgsConstructor
public class FinancialSettingsService {

    private static final String DISCLAIMER = "Rentabilidade estimada. Os valores reais podem variar.";

    private final FinancialSettingsRepository repository;
    private final UserRepository userRepository;

    @Transactional
    public RatesResponse getRates(Long userId) {
        FinancialSettings settings = repository.findByUserId(userId)
                .orElseGet(this::getOrCreateGlobalDefaults);
        return toResponse(settings);
    }

    @Transactional
    public RatesResponse updateRates(Long userId, RatesRequest request) {
        FinancialSettings settings = repository.findByUserId(userId).orElseGet(() ->
                FinancialSettings.builder().user(userRepository.getReferenceById(userId)).build());

        if (request.savingsRate() != null) settings.setSavingsRate(request.savingsRate());
        if (request.cdbRate() != null) settings.setCdbRate(request.cdbRate());
        if (request.treasuryRate() != null) settings.setTreasuryRate(request.treasuryRate());
        if (request.fixedIncomeRate() != null) settings.setFixedIncomeRate(request.fixedIncomeRate());
        if (request.fiiRate() != null) settings.setFiiRate(request.fiiRate());
        if (request.stocksRate() != null) settings.setStocksRate(request.stocksRate());

        // preenche o que não veio no request com o default global, pra nunca salvar campo nulo
        FinancialSettings defaults = getOrCreateGlobalDefaults();
        if (settings.getSavingsRate() == null) settings.setSavingsRate(defaults.getSavingsRate());
        if (settings.getCdbRate() == null) settings.setCdbRate(defaults.getCdbRate());
        if (settings.getTreasuryRate() == null) settings.setTreasuryRate(defaults.getTreasuryRate());
        if (settings.getFixedIncomeRate() == null) settings.setFixedIncomeRate(defaults.getFixedIncomeRate());
        if (settings.getFiiRate() == null) settings.setFiiRate(defaults.getFiiRate());
        if (settings.getStocksRate() == null) settings.setStocksRate(defaults.getStocksRate());

        return toResponse(repository.save(settings));
    }

    private FinancialSettings getOrCreateGlobalDefaults() {
        return repository.findByUserIsNull().orElseGet(() -> repository.save(
                FinancialSettings.builder()
                        .user(null)
                        .savingsRate(new BigDecimal("6.17"))
                        .cdbRate(new BigDecimal("11.5"))
                        .treasuryRate(new BigDecimal("11.0"))
                        .fixedIncomeRate(new BigDecimal("10.5"))
                        .fiiRate(new BigDecimal("9.0"))
                        .stocksRate(new BigDecimal("12.0"))
                        .build()
        ));
    }

    private RatesResponse toResponse(FinancialSettings s) {
        return new RatesResponse(
                s.getSavingsRate(), s.getCdbRate(), s.getTreasuryRate(),
                s.getFixedIncomeRate(), s.getFiiRate(), s.getStocksRate(), DISCLAIMER
        );
    }
}
