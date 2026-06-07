package com.inventory.vehicle.product.infrastructure;

import com.inventory.vehicle.product.domain.VehicleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    Optional<VehicleType> findByNameIgnoreCase(String name);
}
