package imbuy.lot.dto;

public record UserDto(
        Long id,
        String email,
        String username,
        String role
) {}