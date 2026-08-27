package com.financeapp.dto;

import com.financeapp.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryDtos {

    public record CategoryRequest(
            @NotBlank(message = "Nome da categoria é obrigatório") String name,
            @NotNull(message = "Tipo é obrigatório (INCOME ou EXPENSE)") CategoryType type,
            String icon
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            CategoryType type,
            String icon,
            boolean isDefault
    ) {}
}
