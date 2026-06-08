package com.inventory.vehicle.server.product;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    Optional<VehicleType> findByNameIgnoreCase(String name);
}
