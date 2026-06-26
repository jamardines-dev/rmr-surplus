package com.inventory.vehicle.product.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductResult(
        Long id,
        String productName,
        String brandName,
        String vehicleTypeName,
        String modelCode,
        int currentStock,
        BigDecimal unitPrice,
        List<ProductImageResult> images,
        LocalDate lastRestockedDate,
        String lastDrNumber,
        boolean active
) {
}
