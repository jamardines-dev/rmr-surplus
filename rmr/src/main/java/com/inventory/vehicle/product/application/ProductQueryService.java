package com.inventory.vehicle.product.application;

import com.inventory.vehicle.inventory.infrastructure.StockMovementRepository;
import com.inventory.vehicle.product.domain.ProductImage;
import com.inventory.vehicle.product.infrastructure.ProductImageRepository;
import com.inventory.vehicle.product.infrastructure.ProductRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final StockMovementRepository stockMovementRepository;

    public ProductQueryService(
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> findActiveProducts() {
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        var products = productRepository.findByActiveTrueOrderByProductNameAsc();
        Map<Long, ProductImageResult> firstImageByProductId = buildFirstImageMap(products.stream()
                .map(product -> product.getId())
                .toList());
        return products.stream()
                .map(product -> ProductResultMapper.toResult(
                        product,
                        lastDrByProductId.get(product.getId()),
                        imageList(firstImageByProductId.get(product.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResult> findOutOfStockProducts() {
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        var products = productRepository.findByActiveTrueOrderByProductNameAsc()
                .stream()
                .filter(product -> product.getCurrentStock() == 0)
                .toList();
        Map<Long, ProductImageResult> firstImageByProductId = buildFirstImageMap(products.stream()
                .map(product -> product.getId())
                .toList());
        return products.stream()
                .map(product -> ProductResultMapper.toResult(
                        product,
                        lastDrByProductId.get(product.getId()),
                        imageList(firstImageByProductId.get(product.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResult findProduct(Long productId) {
        Map<Long, String> lastDrByProductId = buildLastDrMap();
        return productRepository.findByIdAndActiveTrue(productId)
                .map(product -> ProductResultMapper.toResult(product, lastDrByProductId.get(product.getId())))
                .orElseThrow(() -> new IllegalArgumentException("Product was not found."));
    }

    private Map<Long, ProductImageResult> buildFirstImageMap(List<Long> productIds) {
        Map<Long, ProductImageResult> imageMap = new HashMap<>();
        if (productIds.isEmpty()) {
            return imageMap;
        }
        for (ProductImage image : productImageRepository.findFirstImagesByProductIds(productIds)) {
            imageMap.put(image.getProduct().getId(), new ProductImageResult(
                    image.getId(),
                    image.getImageData(),
                    image.getImageType(),
                    image.getSortOrder()));
        }
        return imageMap;
    }

    private List<ProductImageResult> imageList(ProductImageResult image) {
        return image == null ? List.of() : List.of(image);
    }

    private Map<Long, String> buildLastDrMap() {
        Map<Long, String> drMap = new HashMap<>();
        stockMovementRepository.findRestocksWithDrNumbers()
                .forEach(movement -> drMap.putIfAbsent(movement.getProduct().getId(), movement.getReferenceId()));
        return drMap;
    }
}
