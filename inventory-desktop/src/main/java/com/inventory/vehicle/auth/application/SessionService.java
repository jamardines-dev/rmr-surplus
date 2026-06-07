package com.inventory.vehicle.auth.application;

import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.auth.domain.User;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private User currentUser;

    public void startSession(User user) {
        currentUser = user;
    }

    public void clearSession() {
        currentUser = null;
    }

    public String getCurrentUsername() {
        return currentUser == null ? "system" : currentUser.getUsername();
    }

    public Role getCurrentRole() {
        return currentUser == null ? null : currentUser.getRole();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
