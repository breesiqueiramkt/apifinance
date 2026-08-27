package com.financeapp.controller;

import com.financeapp.dto.FinancialSettingsDtos.RatesRequest;
import com.financeapp.dto.FinancialSettingsDtos.RatesResponse;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.FinancialSettingsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/rates")
@RequiredArgsConstructor
@Tag(name = "Configurações de rentabilidade")
public class FinancialSettingsController {

    private final FinancialSettingsService financialSettingsService;

    @GetMapping
    public RatesResponse get(@AuthenticationPrincipal UserPrincipal user) {
        return financialSettingsService.getRates(user.getId());
    }

    @PutMapping
    public RatesResponse update(@AuthenticationPrincipal UserPrincipal user, @RequestBody RatesRequest request) {
        return financialSettingsService.updateRates(user.getId(), request);
    }
}
