package com.finantialhub.finantialhubapi.domain.validation;

import com.finantialhub.finantialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
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
