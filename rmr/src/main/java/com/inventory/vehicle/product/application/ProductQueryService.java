package com.inventory.vehicle.product.application;

import com.inventory.vehicle.inventory.infrastructure.StockMovementRepository;
import com.inventory.vehicle.product.infrastructure.ProductRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductQueryService(ProductRepository productRepository, StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> findActiveProducts() {
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return productRepository.findByActiveTrueOrderByProductNameAsc()
                .stream()
                .map(product -> ProductResultMapper.toResult(product, lastDrByProductId.get(product.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResult> findOutOfStockProducts() {
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return productRepository.findByActiveTrueOrderByProductNameAsc()
                .stream()
                .filter(product -> product.getCurrentStock() == 0)
                .map(product -> ProductResultMapper.toResult(product, lastDrByProductId.get(product.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResult findProduct(Long productId) {
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return productRepository.findByIdAndActiveTrue(productId)
                .map(product -> ProductResultMapper.toResult(product, lastDrByProductId.get(product.getId())))
                .orElseThrow(() -> new IllegalArgumentException("Product was not found."));
    }

    private Map<Long, String> buildLastDrMap() {
        Map<Long, String> drMap = new HashMap<>();
        stockMovementRepository.findRestocksWithDrNumbers()
                .forEach(movement -> drMap.putIfAbsent(movement.getProduct().getId(), movement.getReferenceId()));
        return drMap;
    }
}
