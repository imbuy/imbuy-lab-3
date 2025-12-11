package imbuy.user.service;

import imbuy.user.domain.Role;
import imbuy.user.domain.User;
import imbuy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class AuthUserService {

    private final UserRepository userRepository;

    public Mono<User> save(User user) {
        return Mono.fromCallable(() -> userRepository.save(user))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Boolean> emailExists(String email) {
        return Mono.fromCallable(() -> userRepository.existsByEmail(email))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<User> findByEmail(String email) {
        return Mono.fromCallable(() -> userRepository.findByEmail(email))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional.map(Mono::just).orElse(Mono.empty()));
    }

    public Mono<User> findById(Long id) {
        return Mono.fromCallable(() -> userRepository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional.map(Mono::just).orElse(Mono.empty()));
    }

    public Mono<Boolean> supervisorExists() {
        return Mono.fromCallable(() -> userRepository.existsByRole(Role.SUPERVISOR))
                .subscribeOn(Schedulers.boundedElastic());
    }
}

