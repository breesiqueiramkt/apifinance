package com.financeapp.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreditCardDtos {

    public record CreditCardRequest(
            @NotBlank(message = "Nome do cartão é obrigatório") String name,
            String bank,
            @NotNull @DecimalMin(value = "0.01") BigDecimal creditLimit,
            @NotNull @Min(1) @Max(31) Short closingDay,
            @NotNull @Min(1) @Max(31) Short dueDay
    ) {}

    public record CreditCardResponse(
            Long id,
            String name,
            String bank,
            BigDecimal creditLimit,
            Short closingDay,
            Short dueDay,
            BigDecimal currentInvoice,
            BigDecimal nextInvoice,
            BigDecimal limitUsed,
            BigDecimal limitAvailable
    ) {}

    public record PurchaseRequest(
            @NotBlank(message = "Descrição é obrigatória") String description,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotNull LocalDate purchaseDate,
            Long categoryId,
            @NotNull @Min(1) @Max(48) Short installments
    ) {}

    public record CreditCardTransactionResponse(
            Long id,
            String description,
            BigDecimal amount,
            LocalDate purchaseDate,
            Short installments,
            Short installmentNo,
            String categoryName
    ) {}
}
