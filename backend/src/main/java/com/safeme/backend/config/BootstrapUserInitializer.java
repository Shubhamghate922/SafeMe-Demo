package com.safeme.backend.config;

import com.safeme.backend.model.UserDocument;
import com.safeme.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapPassword;
    private final String bootstrapFullName;
    private final String bootstrapMobile;

    public BootstrapUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${safeme.bootstrap.email:}") String bootstrapEmail,
            @Value("${safeme.bootstrap.password:}") String bootstrapPassword,
            @Value("${safeme.bootstrap.full-name:SafeMe User}") String bootstrapFullName,
            @Value("${safeme.bootstrap.mobile:}") String bootstrapMobile) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapFullName = bootstrapFullName;
        this.bootstrapMobile = bootstrapMobile;
    }

    @Override
    public void run(String... args) {
        if (bootstrapEmail == null || bootstrapEmail.trim().isEmpty()
                || bootstrapPassword == null || bootstrapPassword.isEmpty()) {
            return;
        }
        String normalizedEmail = bootstrapEmail.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        String mobile = (bootstrapMobile != null && !bootstrapMobile.trim().isEmpty())
                ? bootstrapMobile.replaceAll("[\\s\\-()]", "")
                : "9000000000";

        if (userRepository.existsByMobileNumber(mobile)) {
            log.warn("Bootstrap skipped: mobile number already registered.");
            return;
        }

        UserDocument user = new UserDocument();
        user.setFullName(bootstrapFullName != null && !bootstrapFullName.trim().isEmpty()
                ? bootstrapFullName.trim() : "SafeMe User");
        user.setEmail(normalizedEmail);
        user.setMobileNumber(mobile);
        user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        user.setVerified(true);
        user.setCreatedAt(System.currentTimeMillis());

        userRepository.save(user);
        log.info("Initialized bootstrap email/password user account for {}", normalizedEmail);
    }
}
