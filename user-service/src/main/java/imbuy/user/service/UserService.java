package imbuy.user.service;

import imbuy.user.domain.User;
import imbuy.user.dto.UpdateUserRequest;
import imbuy.user.dto.UserDto;
import imbuy.user.mapper.UserMapper;
import imbuy.user.security.UserPrincipal;
import imbuy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthUserService authUserService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public Flux<UserDto> findAllUsers(Pageable pageable) {
        return Mono.fromCallable(() -> userRepository.findAll(pageable))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapIterable(page -> page.getContent())
                .map(userMapper::mapToDto);
    }

    public Mono<UserDto> findById(Long id, UserPrincipal requester) {
        if (requester != null) {
            requireSelfOrSupervisor(id, requester);
        }
        return authUserService.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .map(userMapper::mapToDto);
    }

    public Mono<UserDto> updateProfile(Long userId, UpdateUserRequest request, UserPrincipal requester) {
        requireSelfOrSupervisor(userId, requester);
        return authUserService.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .flatMap(existingUser -> {
                    User.UserBuilder userBuilder = existingUser.toBuilder()
                            .username(request.username() != null ? request.username() : existingUser.getUsername());

                    if (request.password() != null && !request.password().isEmpty()) {
                        userBuilder.password(passwordEncoder.encode(request.password()));
                    }

                    User updatedUser = userBuilder.build();
                    return authUserService.save(updatedUser)
                            .map(userMapper::mapToDto);
                })
                .doOnSuccess(user -> log.info("User profile updated: id={}", user.id()));
    }

    private void requireSelfOrSupervisor(Long targetUserId, UserPrincipal requester) {
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Unauthorized (Error 401): JWT token is required. Please provide Authorization header with Bearer token");
        }
        if (!requester.isSupervisor() && !requester.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    String.format("Access Denied (Error 403): You can only modify your own profile. " +
                                    "Attempted to modify user ID %d, but you are user ID %d with role %s. " +
                                    "Users with role USER can only modify their own resources.",
                            targetUserId, requester.getId(), requester.getRole()));
        }
    }
}