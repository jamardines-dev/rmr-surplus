package com.inventory.vehicle.server.sales;

import java.math.BigDecimal;

public record SaleItemDto(
        String productName,
        String brand,
        String vehicleType,
        String modelCode,
        int quantity,
        BigDecimal priceSold,
        BigDecimal totalAmount
) {
    static SaleItemDto from(SaleItem item) {
        var product = item.getProduct();
        return new SaleItemDto(
                product.getProductName(),
                product.getBrand().getName(),
                product.getVehicleType().getName(),
                product.getModelCode(),
                item.getQuantitySold(),
                item.getPriceSold(),
                item.getTotalAmount()
        );
    }
}
