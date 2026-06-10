package com.inventory.vehicle.auth.application;

import com.inventory.vehicle.audit.application.AuditService;
import com.inventory.vehicle.audit.domain.AuditLog;
import com.inventory.vehicle.audit.infrastructure.AuditLogRepository;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.auth.domain.User;
import com.inventory.vehicle.auth.infrastructure.UserRepository;
import com.inventory.vehicle.common.exception.BusinessException;
import com.inventory.vehicle.common.security.PasswordHashing;
import com.inventory.vehicle.sales.domain.Sale;
import com.inventory.vehicle.sales.infrastructure.SaleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private static final String LOGIN_ACTION = "LOGIN_SUCCESS";

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SaleRepository saleRepository;
    private final PasswordHashing passwordHashing;
    private final SessionService sessionService;
    private final AuditService auditService;

    public UserManagementService(
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            SaleRepository saleRepository,
            PasswordHashing passwordHashing,
            SessionService sessionService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.saleRepository = saleRepository;
        this.passwordHashing = passwordHashing;
        this.sessionService = sessionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserAccountResult> listUsers() {
        requireAdmin();
        return userRepository.findAllByOrderByUsernameAsc()
                .stream()
                .map(user -> new UserAccountResult(
                        user.getId(),
                        user.getUsername(),
                        user.getRole(),
                        user.isActive(),
                        user.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryResult> loginHistory() {
        requireAdmin();
        return auditLogRepository.findByActionOrderByCreatedAtDesc(LOGIN_ACTION)
                .stream()
                .map(this::toLoginHistory)
                .toList();
    }

    @Transactional
    public void createUser(String username, Role role, boolean active, String password) {
        requireAdmin();
        validateUsername(username);
        if (role == null) {
            throw new BusinessException("Role is required.");
        }
        validateNewPassword(password);
        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new BusinessException("Username is already taken.");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setRole(role);
        user.setActive(active);
        user.setPasswordHash(passwordHashing.hash(password));
        userRepository.save(user);

        auditService.record(
                "CREATE_USER_ACCOUNT",
                "Created account " + user.getUsername() + " (" + user.getRole() + ")",
                sessionService.getCurrentUsername()
        );
    }

    @Transactional
    public void updateUser(Long userId, String username, Role role, boolean active, String newPassword) {
        requireAdmin();
        if (userId == null) {
            throw new BusinessException("Select an account to edit.");
        }
        validateUsername(username);
        if (role == null) {
            throw new BusinessException("Role is required.");
        }
        if (newPassword != null && !newPassword.isBlank()) {
            validateNewPassword(newPassword);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Account was not found."));
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username.trim(), userId)) {
            throw new BusinessException("Username is already taken.");
        }
        if (user.getId().equals(sessionService.getCurrentUserId()) && (!active || role != Role.ADMIN)) {
            throw new BusinessException("You cannot remove admin access from your own account.");
        }

        String oldUsername = user.getUsername();
        String newUsername = username.trim();
        if (!oldUsername.equals(newUsername)) {
            List<Sale> employeeSales = saleRepository.findBySellerNameIgnoreCase(oldUsername);
            for (Sale sale : employeeSales) {
                sale.setSellerName(newUsername);
                if (sale.getEncodedBy().equalsIgnoreCase(oldUsername)) {
                    sale.setEncodedBy(newUsername);
                }
            }
            saleRepository.saveAll(employeeSales);
        }

        user.setUsername(newUsername);
        user.setRole(role);
        user.setActive(active);
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordHashing.hash(newPassword));
        }
        userRepository.save(user);
        if (user.getId().equals(sessionService.getCurrentUserId())) {
            sessionService.updateCurrentUsername(user.getUsername());
        }

        auditService.record(
                "UPDATE_USER_ACCOUNT",
                "Updated account " + oldUsername + " as " + user.getUsername() + " (" + user.getRole() + ")",
                sessionService.getCurrentUsername()
        );
    }

    @Transactional
    public void deleteUser(Long userId) {
        requireAdmin();
        if (userId == null) {
            throw new BusinessException("Select an account to delete.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Account was not found."));
        if (user.getId().equals(sessionService.getCurrentUserId())) {
            throw new BusinessException("You cannot delete your own account.");
        }
        if (user.getRole() == Role.ADMIN && user.isActive() && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new BusinessException("You cannot delete the last active admin account.");
        }

        String username = user.getUsername();
        Role role = user.getRole();
        userRepository.delete(user);
        auditService.record(
                "DELETE_USER_ACCOUNT",
                "Deleted account " + username + " (" + role + ")",
                sessionService.getCurrentUsername()
        );
    }

    private LoginHistoryResult toLoginHistory(AuditLog auditLog) {
        String description = auditLog.getDescription() == null ? "" : auditLog.getDescription();
        String role = description.replace(" logged in", "").trim();
        return new LoginHistoryResult(auditLog.getPerformedBy(), role, auditLog.getCreatedAt());
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("Username is required.");
        }
        if (username.trim().length() < 3) {
            throw new BusinessException("Username must be at least 3 characters.");
        }
    }

    private void validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException("Password is required.");
        }
        if (password.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters.");
        }
    }

    private void requireAdmin() {
        if (!sessionService.isLoggedIn()) {
            throw new BusinessException("You must be logged in.");
        }
        if (sessionService.getCurrentRole() != Role.ADMIN) {
            throw new BusinessException("Only admins can manage user accounts.");
        }
    }
}
