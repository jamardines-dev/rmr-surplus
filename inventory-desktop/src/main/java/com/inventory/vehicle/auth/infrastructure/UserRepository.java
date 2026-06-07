package com.inventory.vehicle.auth.infrastructure;

import com.inventory.vehicle.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCaseAndActiveTrue(String username);
}
