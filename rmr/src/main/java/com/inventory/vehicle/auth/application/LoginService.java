package com.inventory.vehicle.auth.application;

import com.inventory.vehicle.audit.application.AuditService;
import com.inventory.vehicle.auth.domain.User;
import com.inventory.vehicle.auth.infrastructure.UserRepository;
import com.inventory.vehicle.common.security.PasswordHashing;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordHashing passwordHashing;
    private final SessionService sessionService;
    private final AuditService auditService;

    public LoginService(
            UserRepository userRepository,
            PasswordHashing passwordHashing,
            SessionService sessionService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordHashing = passwordHashing;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    public boolean login(String username, String password) {
        Optional<User> user = userRepository.findByUsernameIgnoreCaseAndActiveTrue(username.trim());

        if (user.isEmpty() || !passwordHashing.matches(password, user.get().getPasswordHash())) {
            return false;
        }

        sessionService.startSession(user.get());
        auditService.record(
                "LOGIN_SUCCESS",
                user.get().getRole() + " logged in",
                user.get().getUsername()
        );
        return true;
    }
}
