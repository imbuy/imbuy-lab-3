package imbuy.user.auth;

import imbuy.user.domain.Role;
import imbuy.user.domain.User;
import imbuy.user.dto.AuthResponse;
import imbuy.user.dto.LoginRequest;
import imbuy.user.dto.RefreshTokenRequest;
import imbuy.user.dto.RegisterRequest;
import imbuy.user.repository.UserRepository;
import imbuy.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.security.jwt.secret=dGhpc19pcy1hLWxvbmdlci1iYXNlNjQtand0LXNlY3JldC1rZXk=",
        "springdoc.api-docs.enabled=false"
})
class AuthServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("user_auth_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String userEmail = "user@example.com";
    private final String userPassword = "Secret123!";

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .email("supervisor@test.com")
                .password(passwordEncoder.encode("Supervisor123!"))
                .username("supervisor")
                .role(Role.SUPERVISOR)
                .active(true)
                .build());
    }

    @Test
    void loginShouldReturnTokens() {
        userRepository.save(User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPassword))
                .username("user")
                .role(Role.USER)
                .active(true)
                .build());

        StepVerifier.create(authService.login(new LoginRequest(userEmail, userPassword)))
                .assertNext(response -> {
                    assertThat(response.accessToken()).isNotBlank();
                    assertThat(response.refreshToken()).isNotBlank();
                    assertThat(response.user().email()).isEqualTo(userEmail);
                })
                .verifyComplete();
    }

    @Test
    void registerShouldHashPasswordAndSetRole() {
        RegisterRequest request = new RegisterRequest(
                "newuser@example.com",
                "NewPass123!",
                "newuser",
                Role.USER
        );

        StepVerifier.create(authService.register(request))
                .assertNext(dto -> {
                    assertThat(dto.id()).isNotNull();
                    assertThat(dto.role()).isEqualTo(Role.USER);
                })
                .verifyComplete();

        Optional<User> saved = userRepository.findByEmail("newuser@example.com");
        assertTrue(saved.isPresent());
        assertTrue(passwordEncoder.matches("NewPass123!", saved.get().getPassword()));
    }

    @Test
    void refreshTokenShouldReturnNewAccessToken() {
        userRepository.save(User.builder()
                .email(userEmail)
                .password(passwordEncoder.encode(userPassword))
                .username("user")
                .role(Role.USER)
                .active(true)
                .build());

        AuthResponse login = authService.login(new LoginRequest(userEmail, userPassword)).block();
        assertThat(login).isNotNull();

        StepVerifier.create(authService.refreshToken(new RefreshTokenRequest(login.refreshToken())))
                .assertNext(refreshed -> {
                    assertThat(refreshed.accessToken()).isNotBlank();
                    assertThat(refreshed.refreshToken()).isNotBlank();
                    assertThat(refreshed.user().email()).isEqualTo(userEmail);
                })
                .verifyComplete();
    }
}

