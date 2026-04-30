package com.financialhub.financialhubapi.domain.validation;

import com.financialhub.financialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.financialhub.financialhubapi.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailUniquenessValidatorTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailUniquenessValidator validator = new EmailUniquenessValidator(userRepository);

    @Test
    void emailNotExisting_shouldNotThrow() {
        String email = "newuser@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        assertDoesNotThrow(() ->
                validator.validate(email, "John Doe", "Abcdef12!")
        );

        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existingEmail_shouldThrowEmailAlreadyExistsException() {
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(
                EmailAlreadyExistsException.class,
                () -> validator.validate(email, "John Doe", "Abcdef12!")
        );

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository).existsByEmail(email);
    }
}
