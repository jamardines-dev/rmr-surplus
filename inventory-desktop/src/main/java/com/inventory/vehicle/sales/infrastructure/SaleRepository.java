package com.inventory.vehicle.sales.infrastructure;

import com.inventory.vehicle.sales.domain.Sale;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findBySoldDateOrderByCreatedAtDesc(LocalDate soldDate);

    Optional<Sale> findFirstBySellerNameIgnoreCaseOrderByCreatedAtDesc(String sellerName);

    List<Sale> findBySellerNameIgnoreCase(String sellerName);

    List<Sale> findAllByOrderByCreatedAtDesc();
}
