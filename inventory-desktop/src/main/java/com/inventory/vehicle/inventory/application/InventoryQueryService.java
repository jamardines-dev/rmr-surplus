package com.inventory.vehicle.inventory.application;

import com.inventory.vehicle.inventory.domain.StockMovement;
import com.inventory.vehicle.inventory.infrastructure.StockMovementRepository;
import com.inventory.vehicle.product.application.ProductQueryService;
import com.inventory.vehicle.product.application.ProductResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryQueryService {

    private final ProductQueryService productQueryService;
    private final StockMovementRepository stockMovementRepository;

    public InventoryQueryService(ProductQueryService productQueryService, StockMovementRepository stockMovementRepository) {
        this.productQueryService = productQueryService;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> listInventory() {
        return productQueryService.findActiveProducts();
    }

    @Transactional(readOnly = true)
    public List<StockMovement> findMovementsForProduct(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}
