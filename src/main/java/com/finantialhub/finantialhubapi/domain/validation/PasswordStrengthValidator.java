package com.finantialhub.finantialhubapi.domain.validation;

import com.finantialhub.finantialhubapi.domain.exceptions.WeakPasswordException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordStrengthValidator implements UserRegistrationValidator {

    // At least 8 characters, 1 uppercase letter, 1 digit
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*\\d).{8,}$"
    );

    private static final String WEAK_PASSWORD_MESSAGE =
            "Password must be at least 8 characters long and include at least 1 uppercase letter and 1 digit";

    public void validatePassword(String rawPassword) {
        if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new WeakPasswordException(WEAK_PASSWORD_MESSAGE);
        }
    }

    @Override
    public void validate(String email, String fullName, String rawPassword) {
        validatePassword(rawPassword);
    }
}
