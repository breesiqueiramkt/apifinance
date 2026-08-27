package com.financeapp.controller;

import com.financeapp.dto.AuthDtos.AuthResponse;
import com.financeapp.dto.AuthDtos.LoginRequest;
import com.financeapp.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Não há endpoint de cadastro: o único usuário do app é criado
 * automaticamente na subida da aplicação (ver MasterUserSeeder). Login é a
 * única porta de entrada.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
