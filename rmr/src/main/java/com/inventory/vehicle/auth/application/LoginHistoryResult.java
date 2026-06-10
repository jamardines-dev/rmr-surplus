package com.inventory.vehicle.auth.application;

import java.time.LocalDateTime;

public record LoginHistoryResult(
        String username,
        String role,
        LocalDateTime loggedInAt
) {
}
