package com.inventory.vehicle.sales.infrastructure;

import com.inventory.vehicle.sales.domain.SaleItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySaleId(Long saleId);
}
