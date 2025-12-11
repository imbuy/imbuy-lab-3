package imbuy.user.config;

import imbuy.user.domain.Role;
import imbuy.user.domain.User;
import imbuy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.default-supervisor.email:supervisor@imbuy.local}")
    private String defaultEmail;

    @Value("${app.security.default-supervisor.password:Supervisor123!}")
    private String defaultPassword;

    @Value("${app.security.default-supervisor.username:Supervisor}")
    private String defaultUsername;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSupervisorExists() {
        if (userRepository.existsByRole(Role.SUPERVISOR)) {
            return;
        }

        User supervisor = User.builder()
                .email(defaultEmail)
                .password(passwordEncoder.encode(defaultPassword))
                .username(defaultUsername)
                .role(Role.SUPERVISOR)
                .active(true)
                .build();
        userRepository.save(supervisor);
        log.info("Created default supervisor account with email {}", defaultEmail);
    }
}

