package com.learnapp.config;

import com.learnapp.entities.User;
import com.learnapp.entities.UserRole;
import com.learnapp.entities.UserStatus;
import com.learnapp.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String email;
    private final String password;
    private final String displayName;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.enabled:true}") boolean enabled,
            @Value("${admin.email:}") String email,
            @Value("${admin.password:}") String password,
            @Value("${admin.display-name:Admin}") String displayName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            logger.warn("Admin initializer skipped: admin.email is empty");
            return;
        }
        if (password == null || password.isBlank()) {
            logger.warn("Admin initializer skipped: admin.password is empty");
            return;
        }

        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(existing -> {
            if (existing.getRole() != UserRole.ADMIN) {
                existing.setRole(UserRole.ADMIN);
                userRepository.save(existing);
                logger.info("Admin initializer: upgraded existing user to ADMIN");
            } else {
                logger.info("Admin initializer: admin already exists");
            }
        }, () -> {
            User admin = User.builder()
                    .email(normalizedEmail)
                    .passwordHash(passwordEncoder.encode(password))
                    .displayName(displayName == null || displayName.isBlank() ? "Admin" : displayName.trim())
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            logger.info("Admin initializer: created admin user");
        });
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
