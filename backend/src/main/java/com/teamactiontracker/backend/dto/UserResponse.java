package com.teamactiontracker.backend.dto;

public record UserResponse(
        Long userId,
        String name,
        String email,
        String role,
        String message
) {
}