package com.inventory.vehicle.product.application;

import com.inventory.vehicle.product.domain.Product;

final class ProductResultMapper {

    private ProductResultMapper() {
    }

    static ProductResult toResult(Product product) {
        return new ProductResult(
                product.getId(),
                product.getProductName(),
                product.getBrand().getName(),
                product.getVehicleType().getName(),
                product.getModelCode(),
                product.getCurrentStock(),
                product.getUnitPrice(),
                product.isActive()
        );
    }
}
