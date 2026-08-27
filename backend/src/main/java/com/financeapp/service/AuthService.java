package com.financeapp.service;

import com.financeapp.dto.AuthDtos.AuthResponse;
import com.financeapp.dto.AuthDtos.LoginRequest;
import com.financeapp.entity.User;
import com.financeapp.repository.UserRepository;
import com.financeapp.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Não há mais cadastro público (register) - o único usuário deste app é
 * criado automaticamente na subida da aplicação (ver MasterUserSeeder), a
 * partir das variáveis de ambiente MASTER_USER_*. Login é a única porta de
 * entrada.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("E-mail ou senha incorretos");
        }

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha incorretos"));

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail());
    }
}
