package com.inventory.vehicle.server.auth;

public record LoginResponse(
        Long userId,
        String username,
        Role role
) {
}
