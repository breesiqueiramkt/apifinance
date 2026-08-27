package com.financeapp.service;

import com.financeapp.dto.AuthDtos.LoginRequest;
import com.financeapp.entity.User;
import com.financeapp.repository.UserRepository;
import com.financeapp.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Não há mais cadastro público (RegisterRequest) - o único usuário do app é
 * criado pelo MasterUserSeeder na subida da aplicação. AuthService só
 * precisa autenticar esse usuário.
 */
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, jwtService, authenticationManager);
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest("familia@example.com", "senhaCompartilhada123");
        User user = User.builder().id(1L).name("Família").email("familia@example.com").build();

        when(userRepository.findByEmail("familia@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(1L, "familia@example.com")).thenReturn("fake.jwt.token");

        var response = authService.login(request);

        assertThat(response.token()).isEqualTo("fake.jwt.token");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("familia@example.com");
    }

    @Test
    void loginNormalizesEmailToLowercaseBeforeAuthenticating() {
        LoginRequest request = new LoginRequest("Familia@Example.com", "senhaCompartilhada123");
        User user = User.builder().id(1L).name("Família").email("familia@example.com").build();

        when(userRepository.findByEmail("familia@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any())).thenReturn("fake.jwt.token");

        var response = authService.login(request);

        assertThat(response.email()).isEqualTo("familia@example.com");
    }

    @Test
    void loginRejectsWrongPassword() {
        LoginRequest request = new LoginRequest("familia@example.com", "senhaErrada");

        doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("E-mail ou senha incorretos");
    }
}
