package com.inventory.vehicle.server.security;

import com.inventory.vehicle.server.auth.Role;
import com.inventory.vehicle.server.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

    public User requireUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required.");
    }

    public User requireRole(Role role) {
        User user = requireUser();
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission for this action.");
        }
        return user;
    }
}
