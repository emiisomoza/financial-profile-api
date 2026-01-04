package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.domain.validation.UserRegistrationValidator;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final List<UserRegistrationValidator> validators;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        List<UserRegistrationValidator> validators,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.validators = validators;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(String email, String fullName, String rawPassword) {
        validators.forEach(v -> v.validate(email, fullName, rawPassword));

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = User.createNew(email, fullName, passwordHash);
        return userRepository.save(user);
    }

    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}
