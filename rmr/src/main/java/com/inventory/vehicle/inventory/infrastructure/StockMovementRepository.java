package com.inventory.vehicle.inventory.infrastructure;

import com.inventory.vehicle.inventory.domain.StockMovement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("""
            select movement
            from StockMovement movement
            join fetch movement.product product
            join fetch product.brand
            join fetch product.vehicleType
            where movement.movementType = com.inventory.vehicle.inventory.domain.StockMovementType.RESTOCK
                and movement.referenceId is not null
                and trim(movement.referenceId) <> ''
            order by movement.createdAt desc
            """)
    List<StockMovement> findRestocksWithDrNumbers();

    @Query("""
            select movement
            from StockMovement movement
            join fetch movement.product product
            join fetch product.brand
            join fetch product.vehicleType
            where movement.id = :movementId
            """)
    Optional<StockMovement> findByIdWithProduct(Long movementId);
}
