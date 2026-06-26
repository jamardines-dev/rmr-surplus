package com.inventory.vehicle.product.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateProductCommand(
        String productName,
        String brandName,
        String vehicleTypeName,
        String modelCode,
        int currentStock,
        BigDecimal unitPrice,
        List<NewProductImage> images,
        LocalDate lastRestockedDate,
        String drNumber
) {
}
