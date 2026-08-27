package com.financeapp.controller;

import com.financeapp.dto.DebtDtos.*;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.DebtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debts")
@RequiredArgsConstructor
@Tag(name = "Dívidas")
public class DebtController {

    private final DebtService debtService;

    @GetMapping
    public List<DebtResponse> list(@AuthenticationPrincipal UserPrincipal user) {
        return debtService.list(user.getId());
    }

    @GetMapping("/summary")
    public DebtSummary summary(@AuthenticationPrincipal UserPrincipal user) {
        return debtService.summary(user.getId());
    }

    @PostMapping
    public ResponseEntity<DebtResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                                 @Valid @RequestBody DebtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(debtService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    public DebtResponse update(@AuthenticationPrincipal UserPrincipal user,
                                @PathVariable Long id,
                                @Valid @RequestBody DebtRequest request) {
        return debtService.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        debtService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
