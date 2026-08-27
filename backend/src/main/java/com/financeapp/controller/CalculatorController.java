package com.financeapp.controller;

import com.financeapp.dto.CalculatorDtos.*;
import com.financeapp.service.CalculatorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calculators")
@RequiredArgsConstructor
@Tag(name = "Calculadoras financeiras")
public class CalculatorController {

    private final CalculatorService calculatorService;

    @PostMapping("/compound-interest")
    public CompoundInterestResponse compoundInterest(@Valid @RequestBody CompoundInterestRequest request) {
        return calculatorService.compoundInterest(request);
    }

    @PostMapping("/financial-independence")
    public FinancialIndependenceResponse financialIndependence(@Valid @RequestBody FinancialIndependenceRequest request) {
        return calculatorService.financialIndependence(request);
    }

    @PostMapping("/emergency-fund")
    public EmergencyFundResponse emergencyFund(@Valid @RequestBody EmergencyFundRequest request) {
        return calculatorService.emergencyFund(request);
    }

    @PostMapping("/inflation")
    public InflationResponse inflation(@Valid @RequestBody InflationRequest request) {
        return calculatorService.inflation(request);
    }

    @PostMapping("/financing")
    public FinancingResponse financing(@Valid @RequestBody FinancingRequest request) {
        return calculatorService.financing(request);
    }

    @PostMapping("/retirement")
    public RetirementResponse retirement(@Valid @RequestBody RetirementRequest request) {
        return calculatorService.retirement(request);
    }
}
