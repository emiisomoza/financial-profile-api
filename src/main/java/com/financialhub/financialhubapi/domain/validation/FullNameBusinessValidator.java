package com.financialhub.financialhubapi.domain.validation;

import com.financialhub.financialhubapi.domain.exceptions.InvalidFullNameException;
import org.springframework.stereotype.Component;

@Component
public class FullNameBusinessValidator implements UserRegistrationValidator {

    @Override
    public void validate(String email, String fullName, String rawPassword) {
        if (fullName != null && fullName.matches(".*\\d.*")) {
            throw new InvalidFullNameException("Full name cannot contain numbers");
        }
    }
}
