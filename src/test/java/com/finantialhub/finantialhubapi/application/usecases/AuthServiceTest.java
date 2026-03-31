package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.exceptions.InvalidCredentialsException;
import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void authenticate_shouldReturnUser_whenCredentialsAreValid() {
        String email = "user@example.com";
        String rawPassword = "Abcdef12!";
        String hashFromDb = "$2a$10$ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcd";

        User user = new User(
                UUID.randomUUID(),
                email,
                "John Doe",
                hashFromDb,
                Instant.now(),
                Role.MEMBER
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, hashFromDb)).thenReturn(true);

        AuthService authService = new AuthService(userRepository, passwordEncoder);

        User result = authService.authenticate(email, rawPassword);

        assertThat(result).isEqualTo(user);
    }

    @Test
    void authenticate_shouldThrowInvalidCredentials_whenPasswordDoesNotMatch() {
        String email = "user@example.com";
        String rawPassword = "wrongPass";
        String hashFromDb = "$2a$10$ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcd";

        User user = new User(
                UUID.randomUUID(),
                email,
                "John Doe",
                hashFromDb,
                Instant.now(),
                Role.MEMBER
        );

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, hashFromDb)).thenReturn(false);

        AuthService authService = new AuthService(userRepository, passwordEncoder);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.authenticate(email, rawPassword));
    }

    @Test
    void authenticate_shouldThrowInvalidCredentials_whenEmailDoesNotExist() {
        String email = "missing@example.com";
        String rawPassword = "Abcdef12!";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        AuthService authService = new AuthService(userRepository, passwordEncoder);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.authenticate(email, rawPassword));
    }
}
