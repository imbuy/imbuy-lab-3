package imbuy.user.controller;

import imbuy.user.dto.UpdateUserRequest;
import imbuy.user.dto.UserDto;
import imbuy.user.security.UserPrincipal;
import imbuy.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Get paginated list of users (supervisor only)")
    public Flux<UserDto> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return userService.findAllUsers(pageable);
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Get user by ID")
    public Mono<UserDto> getUserById(@PathVariable Long id, Authentication authentication) {
        return userService.findById(id, principal(authentication));
    }

    @PutMapping("/update/{id}/profile")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update user profile")
    public Mono<UserDto> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        return userService.updateProfile(id, request, principal(authentication));
    }

    private UserPrincipal principal(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        return (UserPrincipal) authentication.getPrincipal();
    }
}