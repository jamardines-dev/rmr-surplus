package com.inventory.vehicle.product.application;

import java.math.BigDecimal;

public record CreateProductCommand(
        String productName,
        String brandName,
        String vehicleTypeName,
        String modelCode,
        int currentStock,
        BigDecimal unitPrice
) {
}
