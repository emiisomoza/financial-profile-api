package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.exceptions.UserNotFoundException;
import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.domain.validation.PasswordStrengthValidator;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServicePromoteUserTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private PasswordStrengthValidator passwordStrengthValidator;

    @Test
    void promoteUser_shouldPromoteMemberToAdmin() {
        UUID id = UUID.randomUUID();

        User existing = new User(
                id,
                "member@example.com",
                "John Member",
                "hash",
                Instant.now(),
                Role.MEMBER
        );

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.save(savedCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserService service = new UserService(userRepository, java.util.List.of(), passwordEncoder, passwordStrengthValidator);

        User result = service.promoteUserToAdmin(id);

        User saved = savedCaptor.getValue();

        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void promoteUser_shouldNotChangeRoleIfAlreadyAdmin() {
        UUID id = UUID.randomUUID();

        User admin = new User(
                id,
                "admin@example.com",
                "Admin User",
                "hash",
                Instant.now(),
                Role.ADMIN
        );

        when(userRepository.findById(id)).thenReturn(Optional.of(admin));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.save(savedCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserService service = new UserService(userRepository, java.util.List.of(), passwordEncoder, passwordStrengthValidator);

        User result = service.promoteUserToAdmin(id);

        User saved = savedCaptor.getValue();

        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void promoteUser_shouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserService service = new UserService(userRepository, java.util.List.of(), passwordEncoder, passwordStrengthValidator);

        assertThrows(UserNotFoundException.class,
                () -> service.promoteUserToAdmin(id));
    }
}
