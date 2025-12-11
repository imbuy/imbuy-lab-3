package imbuy.user.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {
}

