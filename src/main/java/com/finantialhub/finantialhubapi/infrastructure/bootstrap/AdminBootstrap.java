package com.finantialhub.finantialhubapi.infrastructure.bootstrap;

import com.finantialhub.finantialhubapi.domain.model.Role;
import com.finantialhub.finantialhubapi.domain.model.User;
import com.finantialhub.finantialhubapi.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final String adminEmail;

    public AdminBootstrap(UserRepository userRepository,
                          @Value("${ADMIN_EMAIL:}") String adminEmail) {
        this.userRepository = userRepository;
        this.adminEmail = adminEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank()) {
            return;
        }

        Optional<User> found = userRepository.findByEmail(adminEmail);

        if (found.isEmpty()) {
            log.warn("AdminBootstrap: no user found with email '{}' — skipping promotion", adminEmail);
            return;
        }

        User user = found.get();

        if (user.getRole() == Role.ADMIN) {
            log.info("AdminBootstrap: '{}' is already ADMIN", adminEmail);
            return;
        }

        userRepository.save(user.promoteToAdmin());
        log.info("AdminBootstrap: '{}' promoted to ADMIN", adminEmail);
    }
}
