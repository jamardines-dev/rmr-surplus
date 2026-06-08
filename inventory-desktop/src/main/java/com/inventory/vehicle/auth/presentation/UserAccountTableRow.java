package com.inventory.vehicle.auth.presentation;

import com.inventory.vehicle.auth.application.UserAccountResult;
import com.inventory.vehicle.auth.domain.Role;
import java.time.format.DateTimeFormatter;

public class UserAccountTableRow {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final Long id;
    private final String username;
    private final Role role;
    private final String status;
    private final boolean active;
    private final String createdAt;

    public UserAccountTableRow(UserAccountResult user) {
        this.id = user.id();
        this.username = user.username();
        this.role = user.role();
        this.active = user.active();
        this.status = user.active() ? "Active" : "Inactive";
        this.createdAt = user.createdAt().format(FORMATTER);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
