package com.inventory.vehicle.product.application;

import com.inventory.vehicle.product.domain.Product;
import java.util.List;

final class ProductResultMapper {

    private ProductResultMapper() {
    }

    static ProductResult toResult(Product product) {
        return toResult(product, null);
    }

    static ProductResult toResult(Product product, String lastDrNumber) {
        List<ProductImageResult> images = product.getImages().stream()
                .map(img -> new ProductImageResult(
                        img.getId(),
                        img.getImageData(),
                        img.getImageType(),
                        img.getSortOrder()))
                .toList();

        return toResult(product, lastDrNumber, images);
    }

    static ProductResult toResult(Product product, String lastDrNumber, List<ProductImageResult> images) {
        return new ProductResult(
                product.getId(),
                product.getProductName(),
                product.getBrand().getName(),
                product.getVehicleType().getName(),
                product.getModelCode(),
                product.getProductLocation(),
                product.getCurrentStock(),
                product.getUnitPrice(),
                images,
                product.getLastRestockedDate(),
                lastDrNumber,
                product.isActive()
        );
    }
}
