package com.inventory.vehicle.server.product;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String productName,
        String brand,
        String vehicleType,
        String modelCode,
        String productLocation,
        int currentStock,
        BigDecimal unitPrice,
        boolean active
) {
    public static ProductDto from(Product product) {
        return new ProductDto(
                product.getId(),
                product.getProductName(),
                product.getBrand().getName(),
                product.getVehicleType().getName(),
                product.getModelCode(),
                product.getProductLocation(),
                product.getCurrentStock(),
                product.getUnitPrice(),
                product.isActive()
        );
    }
}
