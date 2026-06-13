package com.inventory.vehicle.auth.presentation;

import com.inventory.vehicle.auth.application.LoginHistoryResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoginHistoryTableRow {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private final String username;
    private final String role;
    private final String loginDate;
    private final String loginTime;
    private final LocalDateTime loggedInAt;

    public LoginHistoryTableRow(LoginHistoryResult loginHistory) {
        this.username = loginHistory.username();
        this.role = loginHistory.role();
        this.loggedInAt = loginHistory.loggedInAt();
        this.loginDate = loginHistory.loggedInAt().format(DATE_FORMATTER);
        this.loginTime = loginHistory.loggedInAt().format(TIME_FORMATTER);
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getLoginDate() {
        return loginDate;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLoggedInAt() {
        return loggedInAt;
    }
}
