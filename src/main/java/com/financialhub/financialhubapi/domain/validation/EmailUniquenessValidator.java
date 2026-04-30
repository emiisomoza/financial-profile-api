package com.financialhub.financialhubapi.domain.validation;

import com.financialhub.financialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.financialhub.financialhubapi.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class EmailUniquenessValidator implements UserRegistrationValidator {

    private final UserRepository userRepository;

    public EmailUniquenessValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void validate(String email, String fullName, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
    }
}
