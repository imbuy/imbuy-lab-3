package imbuy.user.dto;

import imbuy.user.domain.Role;

public record UserDto(
        Long id,
        String email,
        String username,
        Role role
) {}