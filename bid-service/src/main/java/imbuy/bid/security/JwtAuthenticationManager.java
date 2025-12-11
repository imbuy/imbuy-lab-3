package imbuy.bid.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        try {
            String token = authentication.getCredentials().toString();
            if (!jwtService.isTokenValid(token)) {
                return Mono.error(new org.springframework.security.authentication.BadCredentialsException(
                        "Invalid or expired JWT token"));
            }

            String username = jwtService.extractUsername(token);
            Long userId = jwtService.extractUserId(token);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username,
                    token,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            authToken.setDetails(userId);

            return Mono.just(authToken);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return Mono.error(e);
        } catch (Exception e) {
            return Mono.error(new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid JWT token: " + e.getMessage()));
        }
    }
}

