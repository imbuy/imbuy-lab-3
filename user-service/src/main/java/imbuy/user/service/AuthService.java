package imbuy.user.service;

import imbuy.user.domain.Role;
import imbuy.user.domain.User;
import imbuy.user.dto.AuthResponse;
import imbuy.user.dto.LoginRequest;
import imbuy.user.dto.RefreshTokenRequest;
import imbuy.user.dto.RegisterRequest;
import imbuy.user.dto.UserDto;
import imbuy.user.mapper.UserMapper;
import imbuy.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserService authUserService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Mono<AuthResponse> login(LoginRequest request) {
        return authUserService.findByEmail(request.email())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(user -> {
                    if (!Boolean.TRUE.equals(user.getActive())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is deactivated"));
                    }
                    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
                    }
                    return Mono.just(buildAuthResponse(user));
                });
    }

    public Mono<UserDto> register(RegisterRequest request) {
        Role role = request.role() != null ? request.role() : Role.USER;

        return authUserService.emailExists(request.email())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));
                    }

                    User user = User.builder()
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .username(request.username())
                            .role(role)
                            .active(true)
                            .build();

                    return authUserService.save(user)
                            .map(userMapper::mapToDto)
                            .doOnSuccess(dto -> log.info("User registered: {}", dto.email()));
                });
    }

    public Mono<AuthResponse> refreshToken(RefreshTokenRequest request) {
        if (!jwtService.isRefreshToken(request.refreshToken())) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        }
        String email = jwtService.extractUsername(request.refreshToken());
        return authUserService.findByEmail(email)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")))
                .flatMap(user -> {
                    if (!jwtService.isTokenValid(request.refreshToken(), user.getEmail())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
                    }
                    return Mono.just(buildAuthResponse(user));
                });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, userMapper.mapToDto(user));
    }
}

