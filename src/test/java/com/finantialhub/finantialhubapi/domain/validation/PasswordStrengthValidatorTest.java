package com.finantialhub.finantialhubapi.domain.validation;

import com.finantialhub.finantialhubapi.domain.exceptions.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordStrengthValidatorTest {

    private final PasswordStrengthValidator validator = new PasswordStrengthValidator();

    @Test
    void validPassword_shouldNotThrow() {
        // at least 8 chars, 2 digits, 1 uppercase, 1 special
        String password = "Abcdef12!";

        assertDoesNotThrow(() ->
                validator.validate("test@example.com", "John Doe", password)
        );
    }

    @Test
    void tooShortPassword_shouldThrowWeakPasswordException() {
        String password = "A1!a"; // too short

        WeakPasswordException ex = assertThrows(
                WeakPasswordException.class,
                () -> validator.validate("test@example.com", "John Doe", password)
        );

        assertTrue(ex.getMessage().contains("Password must be at least 8 characters"));
    }

    @Test
    void passwordWithNoUppercase_shouldThrowWeakPasswordException() {
        String password = "abcdef12!";

        assertThrows(
                WeakPasswordException.class,
                () -> validator.validate("test@example.com", "John Doe", password)
        );
    }

    @Test
    void passwordWithLessThanTwoDigits_shouldThrowWeakPasswordException() {
        String password = "Abcdef1!"; // only 1 digit

        assertThrows(
                WeakPasswordException.class,
                () -> validator.validate("test@example.com", "John Doe", password)
        );
    }

    @Test
    void passwordWithoutSpecialCharacter_shouldThrowWeakPasswordException() {
        String password = "Abcdef12"; // no special char

        assertThrows(
                WeakPasswordException.class,
                () -> validator.validate("test@example.com", "John Doe", password)
        );
    }
}
