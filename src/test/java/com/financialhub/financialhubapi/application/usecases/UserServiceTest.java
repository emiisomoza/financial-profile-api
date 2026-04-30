package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.financialhub.financialhubapi.domain.exceptions.UserNotFoundException;
import com.financialhub.financialhubapi.domain.model.Role;
import com.financialhub.financialhubapi.domain.model.User;
import com.financialhub.financialhubapi.domain.validation.PasswordStrengthValidator;
import com.financialhub.financialhubapi.domain.validation.UserRegistrationValidator;
import com.financialhub.financialhubapi.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRegistrationValidator validator1;

    @Mock
    private PasswordStrengthValidator passwordStrengthValidator;

    @Test
    void registerUser_shouldHashPasswordAndSaveUser() {
        String email = "test@example.com";
        String fullName = "John Doe";
        String rawPassword = "Abcdef12!";

        String hashedPassword = "$2a$10$ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcd";

        // when passwordEncoder.encode() is called, return a fake hash
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);

        // capture user passed to repository.save
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        UserService userService = new UserService(
                userRepository,
                List.of(validator1),
                passwordEncoder,
                passwordStrengthValidator
        );

        User result = userService.registerUser(email, fullName, rawPassword);

        // verify validators are called
        verify(validator1).validate(email, fullName, rawPassword);

        // verify password was encoded
        verify(passwordEncoder).encode(rawPassword);

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getFullName()).isEqualTo(fullName);
        assertThat(savedUser.getPasswordHash()).isEqualTo(hashedPassword);
        // ensure raw password is NOT stored
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(rawPassword);

        // also verify service returns same user as saved
        assertThat(result.getPasswordHash()).isEqualTo(hashedPassword);
    }

    @Test
    void updateUser_shouldUpdateEmailAndFullName() {
        UUID id = UUID.randomUUID();

        User existingUser = new User(
                id,
                "old@example.com",
                "Old Name",
                "hash",
                Instant.now(),
                Role.MEMBER
        );

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        UserService service = new UserService(userRepository, List.of(validator1), passwordEncoder, passwordStrengthValidator);

        User result = service.updateUser(id, "new@example.com", "New Name");

        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getFullName()).isEqualTo("New Name");
        assertThat(saved.getPasswordHash()).isEqualTo("hash");

        assertThat(result.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void updateUser_shouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserService service = new UserService(userRepository, List.of(validator1), passwordEncoder, passwordStrengthValidator);

        assertThrows(UserNotFoundException.class,
                () -> service.updateUser(id, "new@example.com", "New Name"));
    }

    @Test
    void updateUser_shouldThrowWhenEmailAlreadyExistsForAnotherUser() {
        UUID id = UUID.randomUUID();

        User existing = new User(
                id,
                "old@example.com",
                "Old Name",
                "hash",
                Instant.now(),
                Role.MEMBER
        );

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UserService service = new UserService(userRepository, List.of(validator1), passwordEncoder, passwordStrengthValidator);

        assertThrows(EmailAlreadyExistsException.class,
                () -> service.updateUser(id, "taken@example.com", "New Name"));
    }
}
