package com.inventory.vehicle.product.infrastructure;

import com.inventory.vehicle.product.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    List<Product> findByActiveTrueOrderByProductNameAsc();

    Optional<Product> findByModelCodeIgnoreCase(String modelCode);

    boolean existsByModelCodeIgnoreCase(String modelCode);

    boolean existsByModelCodeIgnoreCaseAndIdNot(String modelCode, Long id);
}
