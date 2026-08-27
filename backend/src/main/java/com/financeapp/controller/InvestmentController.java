package com.financeapp.controller;

import com.financeapp.dto.InvestmentDtos.*;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.InvestmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
@Tag(name = "Investimentos")
public class InvestmentController {

    private final InvestmentService investmentService;

    @GetMapping("/types")
    public List<InvestmentTypeResponse> types() {
        return investmentService.listTypes();
    }

    @GetMapping
    public List<InvestmentResponse> list(@AuthenticationPrincipal UserPrincipal user) {
        return investmentService.list(user.getId());
    }

    @GetMapping("/summary")
    public InvestmentSummary summary(@AuthenticationPrincipal UserPrincipal user) {
        return investmentService.summary(user.getId());
    }

    @PostMapping
    public ResponseEntity<InvestmentResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                                        @Valid @RequestBody InvestmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investmentService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    public InvestmentResponse update(@AuthenticationPrincipal UserPrincipal user,
                                      @PathVariable Long id,
                                      @Valid @RequestBody InvestmentRequest request) {
        return investmentService.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        investmentService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
