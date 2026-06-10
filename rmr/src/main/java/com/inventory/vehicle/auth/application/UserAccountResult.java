package com.inventory.vehicle.auth.application;

import com.inventory.vehicle.auth.domain.Role;
import java.time.LocalDateTime;

public record UserAccountResult(
        Long id,
        String username,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
}
