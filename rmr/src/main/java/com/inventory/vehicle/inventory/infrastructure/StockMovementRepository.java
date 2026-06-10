package com.inventory.vehicle.inventory.infrastructure;

import com.inventory.vehicle.inventory.domain.StockMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
}
