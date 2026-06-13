package com.inventory.vehicle.inventory.application;

import com.inventory.vehicle.auth.application.SessionService;
import com.inventory.vehicle.auth.domain.Role;
import com.inventory.vehicle.common.exception.BusinessException;
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
    private final SessionService sessionService;

    public InventoryQueryService(
            ProductQueryService productQueryService,
            StockMovementRepository stockMovementRepository,
            SessionService sessionService
    ) {
        this.productQueryService = productQueryService;
        this.stockMovementRepository = stockMovementRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> listInventory() {
        requireAdmin();
        return productQueryService.findActiveProducts();
    }

    @Transactional(readOnly = true)
    public List<StockMovement> findMovementsForProduct(Long productId) {
        requireAdmin();
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional(readOnly = true)
    public List<StockMovement> findDrRestockMovements() {
        requireAdmin();
        return stockMovementRepository.findRestocksWithDrNumbers();
    }

    private void requireAdmin() {
        if (!sessionService.isLoggedIn()) {
            throw new BusinessException("You must be logged in.");
        }
        if (sessionService.getCurrentRole() != Role.ADMIN) {
            throw new BusinessException("Only admins can view stock movements.");
        }
    }
}
