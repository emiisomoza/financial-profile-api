package com.finantialhub.finantialhubapi.domain.validation;

import com.finantialhub.finantialhubapi.domain.exceptions.WeakPasswordException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordStrengthValidator implements UserRegistrationValidator {

    // At least:
    // - 8 characters
    // - 2 digits
    // - 1 uppercase
    // - 1 special character
    // Note: escaped properly for Java string
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=(?:.*\\d){2,})(?=.*[!@#$%^&*()_+\\-\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    );

    @Override
    public void validate(String email, String fullName, String rawPassword) {
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new WeakPasswordException(
                    "Password must be at least 8 characters long and include " +
                            "at least 2 digits, 1 uppercase letter and 1 special character"
            );
        }
    }
}
