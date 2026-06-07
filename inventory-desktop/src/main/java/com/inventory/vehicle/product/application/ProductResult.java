package com.inventory.vehicle.product.application;

import java.math.BigDecimal;

public record ProductResult(
        Long id,
        String productName,
        String brandName,
        String vehicleTypeName,
        int currentStock,
        BigDecimal unitPrice
) {
}
