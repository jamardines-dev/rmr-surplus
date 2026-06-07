package com.inventory.vehicle.product.application;

import com.inventory.vehicle.product.infrastructure.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> findActiveProducts() {
        return productRepository.findByActiveTrueOrderByProductNameAsc()
                .stream()
                .map(ProductResultMapper::toResult)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResult findProduct(Long productId) {
        return productRepository.findByIdAndActiveTrue(productId)
                .map(ProductResultMapper::toResult)
                .orElseThrow(() -> new IllegalArgumentException("Product was not found."));
    }
}
