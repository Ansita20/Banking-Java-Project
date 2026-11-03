package bankproject.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import bankproject.identity.domain.Role;
import bankproject.identity.domain.UserAccount;
import bankproject.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;

/** Creates the first back-office administrator from configuration if none exists yet. */
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${bank.admin.email:}")
    private String adminEmail;

    @Value("${bank.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank() || users.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        UserAccount admin = UserAccount.register(adminEmail.toLowerCase(), passwordEncoder.encode(adminPassword),
                "Bank", "Administrator", Role.ADMIN);
        admin.setEmailVerified(true);
        users.save(admin);
        log.info("Created bootstrap administrator {}", adminEmail);
    }
}
