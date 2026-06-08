package com.inventory.vehicle.auth.infrastructure;

import com.inventory.vehicle.auth.domain.User;
import com.inventory.vehicle.auth.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCaseAndActiveTrue(String username);

    List<User> findAllByOrderByUsernameAsc();

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    long countByRoleAndActiveTrue(Role role);
}
