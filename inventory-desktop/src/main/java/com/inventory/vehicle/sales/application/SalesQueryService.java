package com.inventory.vehicle.sales.application;

import com.inventory.vehicle.sales.domain.Sale;
import com.inventory.vehicle.sales.infrastructure.SaleRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesQueryService {

    private final SaleRepository saleRepository;

    public SalesQueryService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public List<Sale> findSalesByDate(LocalDate soldDate) {
        return saleRepository.findBySoldDateOrderByCreatedAtDesc(soldDate);
    }

    @Transactional(readOnly = true)
    public List<Sale> findAllSales() {
        return saleRepository.findAllByOrderByCreatedAtDesc();
    }
}
