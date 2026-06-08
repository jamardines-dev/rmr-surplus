package com.inventory.vehicle.sales.infrastructure;

import com.inventory.vehicle.sales.domain.SaleItem;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);

    List<SaleItem> findBySaleSoldDateOrderBySaleCreatedAtDesc(LocalDate soldDate);

    List<SaleItem> findBySaleSellerNameIgnoreCaseAndSaleSoldDateOrderBySaleCreatedAtDesc(String sellerName, LocalDate soldDate);

    List<SaleItem> findBySaleSellerNameIgnoreCaseAndSaleSoldDateBetweenOrderBySaleCreatedAtDesc(
            String sellerName,
            LocalDate startDate,
            LocalDate endDate
    );

    List<SaleItem> findBySaleSellerNameIgnoreCaseOrderBySaleCreatedAtDesc(String sellerName);
}
