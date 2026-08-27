package com.financeapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Não existe mais RegisterRequest/cadastro público: este app tem um único
 * usuário (o "usuário master"), criado automaticamente na subida da
 * aplicação a partir das variáveis de ambiente MASTER_USER_* - ver
 * {@link com.financeapp.config.MasterUserSeeder}. Isso garante que só quem
 * tem as credenciais compartilhadas (você e sua esposa) consegue entrar.
 */
public class AuthDtos {

    public record LoginRequest(
            @NotBlank @Email(message = "E-mail inválido") String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String name,
            String email
    ) {}
}
