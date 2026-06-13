package com.inventory.vehicle.product.application;

import java.math.BigDecimal;

public record UpdateDrRestockLineCommand(
        Long movementId,
        String productName,
        String brandName,
        String vehicleTypeName,
        String modelCode,
        int quantity,
        BigDecimal unitPrice
) {
}
