package com.financialhub.financialhubapi.application.usecases;

import com.financialhub.financialhubapi.domain.exceptions.EmailAlreadyExistsException;
import com.financialhub.financialhubapi.domain.exceptions.InvalidPasswordException;
import com.financialhub.financialhubapi.domain.exceptions.UserNotFoundException;
import com.financialhub.financialhubapi.domain.model.User;
import com.financialhub.financialhubapi.domain.validation.PasswordStrengthValidator;
import com.financialhub.financialhubapi.domain.validation.UserRegistrationValidator;
import com.financialhub.financialhubapi.infrastructure.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final List<UserRegistrationValidator> validators;
    private final PasswordEncoder passwordEncoder;
    private final PasswordStrengthValidator passwordStrengthValidator;

    public UserService(
        UserRepository userRepository,
        List<UserRegistrationValidator> validators,
        PasswordEncoder passwordEncoder,
        PasswordStrengthValidator passwordStrengthValidator
    ) {
        this.userRepository = userRepository;
        this.validators = validators;
        this.passwordEncoder = passwordEncoder;
        this.passwordStrengthValidator = passwordStrengthValidator;
    }

    @Transactional
    public User registerUser(String email, String fullName, String rawPassword) {
        validators.forEach(v -> v.validate(email, fullName, rawPassword));

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = User.createNew(email, fullName, passwordHash);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    public User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User updateUser(UUID id, String email, String fullName) {
        User existing = getUser(id);

        // If email changed, check uniqueness
        if (!existing.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User updated = existing.withUpdatedProfile(email, fullName);
        return userRepository.save(updated);
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User changePassword(UUID id, String currentPassword, String newPassword, boolean isAdmin) {
        User user = getUser(id);

        if (!isAdmin && !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        passwordStrengthValidator.validatePassword(newPassword);

        String newHash = passwordEncoder.encode(newPassword);
        return userRepository.save(user.withUpdatedPassword(newHash));
    }

    @Transactional
    public User promoteUserToAdmin(UUID userId) {
        User user = getUser(userId); // already throws UserNotFoundException if missing

        User promoted = user.promoteToAdmin();
        return userRepository.save(promoted);
    }
}
