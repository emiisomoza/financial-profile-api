package com.finantialhub.finantialhubapi.domain.validation;

import com.finantialhub.finantialhubapi.domain.exceptions.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordStrengthValidatorTest {

    private final PasswordStrengthValidator validator = new PasswordStrengthValidator();

    @Test
    void validPassword_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validatePassword("Abcdefg1"));
    }

    @Test
    void tooShortPassword_shouldThrowWeakPasswordException() {
        WeakPasswordException ex = assertThrows(
                WeakPasswordException.class,
                () -> validator.validatePassword("Abc1")
        );
        assertTrue(ex.getMessage().contains("Password must be at least 8 characters"));
    }

    @Test
    void passwordWithNoUppercase_shouldThrowWeakPasswordException() {
        assertThrows(WeakPasswordException.class, () -> validator.validatePassword("abcdefg1"));
    }

    @Test
    void passwordWithNoDigit_shouldThrowWeakPasswordException() {
        assertThrows(WeakPasswordException.class, () -> validator.validatePassword("Abcdefgh"));
    }
}
