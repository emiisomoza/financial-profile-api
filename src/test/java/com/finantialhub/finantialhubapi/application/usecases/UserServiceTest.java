package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.domain.validation.UserRegistrationValidator;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRegistrationValidator validator1; // example, we don't care which

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
                List.of(validator1), // we only verify it's called
                passwordEncoder
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
}
