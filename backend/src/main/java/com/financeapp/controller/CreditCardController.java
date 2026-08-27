package com.financeapp.controller;

import com.financeapp.dto.CreditCardDtos.*;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.CreditCardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-cards")
@RequiredArgsConstructor
@Tag(name = "Cartão de crédito")
public class CreditCardController {

    private final CreditCardService creditCardService;

    @GetMapping
    public List<CreditCardResponse> list(@AuthenticationPrincipal UserPrincipal user) {
        return creditCardService.list(user.getId());
    }

    @PostMapping
    public ResponseEntity<CreditCardResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                                        @Valid @RequestBody CreditCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.create(user.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        creditCardService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/purchases")
    public List<CreditCardTransactionResponse> listPurchases(@AuthenticationPrincipal UserPrincipal user,
                                                                @PathVariable Long id) {
        return creditCardService.listPurchases(user.getId(), id);
    }

    @PostMapping("/{id}/purchases")
    public ResponseEntity<List<CreditCardTransactionResponse>> addPurchase(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditCardService.addPurchase(user.getId(), id, request));
    }
}
