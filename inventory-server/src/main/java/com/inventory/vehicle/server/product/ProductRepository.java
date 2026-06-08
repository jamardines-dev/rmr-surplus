package com.inventory.vehicle.server.product;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select p from Product p
            join fetch p.brand
            join fetch p.vehicleType
            order by p.productName
            """)
    List<Product> findAllWithDetails();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p join fetch p.brand join fetch p.vehicleType where p.id = :id")
    Optional<Product> findByIdForSale(Long id);
}
