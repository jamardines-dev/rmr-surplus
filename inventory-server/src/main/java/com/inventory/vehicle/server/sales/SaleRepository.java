package com.inventory.vehicle.server.sales;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
            select distinct s from Sale s
            left join fetch s.items i
            left join fetch i.product p
            left join fetch p.brand
            left join fetch p.vehicleType
            where s.soldDate = :soldDate
            order by s.createdAt desc
            """)
    List<Sale> findBySoldDateWithItems(LocalDate soldDate);

    @Query("""
            select distinct s from Sale s
            left join fetch s.items i
            left join fetch i.product p
            left join fetch p.brand
            left join fetch p.vehicleType
            where s.soldDate = :soldDate and lower(s.sellerName) = lower(:sellerName)
            order by s.createdAt desc
            """)
    List<Sale> findBySoldDateAndSellerNameWithItems(LocalDate soldDate, String sellerName);
}
