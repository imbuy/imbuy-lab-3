package imbuy.user.security;

import imbuy.user.service.AuthUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final AuthUserService authUserService;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return authUserService.findByEmail(username)
                .map(UserPrincipal::new);
    }
}

