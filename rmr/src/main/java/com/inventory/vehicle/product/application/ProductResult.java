package com.inventory.vehicle.product.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResult(
        Long id,
        String productName,
        String brandName,
        String vehicleTypeName,
        String modelCode,
        int currentStock,
        BigDecimal unitPrice,
        byte[] productImage,
        String productImageType,
        LocalDate lastRestockedDate,
        boolean active
) {
}
