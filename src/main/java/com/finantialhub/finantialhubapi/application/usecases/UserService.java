package com.finantialhub.finantialhubapi.application.usecases;

import com.finantialhub.finantialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    // later: inject PasswordEncoder, etc.

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User registerUser(String email, String fullName, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = User.createNew(email, fullName, rawPassword);
        return userRepository.save(user);
    }

    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }
}
