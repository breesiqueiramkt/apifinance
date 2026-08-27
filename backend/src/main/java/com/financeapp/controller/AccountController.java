package com.financeapp.controller;

import com.financeapp.dto.AccountDtos.AccountRequest;
import com.financeapp.dto.AccountDtos.AccountResponse;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Contas bancárias")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal UserPrincipal user) {
        return accountService.listForUser(user.getId());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                                    @Valid @RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    public AccountResponse update(@AuthenticationPrincipal UserPrincipal user,
                                   @PathVariable Long id,
                                   @Valid @RequestBody AccountRequest request) {
        return accountService.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        accountService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
