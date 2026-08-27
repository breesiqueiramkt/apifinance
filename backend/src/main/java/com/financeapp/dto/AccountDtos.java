package com.financeapp.dto;

import com.financeapp.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountDtos {

    public record AccountRequest(
            @NotBlank(message = "Nome da conta é obrigatório") String name,
            String bank,
            @NotNull(message = "Tipo da conta é obrigatório") AccountType type,
            @NotNull(message = "Saldo inicial é obrigatório") BigDecimal balance,
            String color
    ) {}

    public record AccountResponse(
            Long id,
            String name,
            String bank,
            AccountType type,
            BigDecimal balance,
            String color,
            LocalDateTime createdAt
    ) {}
}
