package com.inventory.vehicle.sales.application;

import com.inventory.vehicle.sales.infrastructure.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailySalesService {

    private final SaleRepository saleRepository;

    public DailySalesService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalForDate(LocalDate soldDate) {
        return saleRepository.findBySoldDateOrderByCreatedAtDesc(soldDate)
                .stream()
                .map(sale -> sale.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
