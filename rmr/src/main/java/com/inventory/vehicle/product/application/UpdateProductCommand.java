package com.inventory.vehicle.product.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateProductCommand(
        Long productId,
        String productName,
        String brandName,
        String vehicleTypeName,
        String modelCode,
        String productLocation,
        int currentStock,
        BigDecimal unitPrice,
        List<Long> removedImageIds,
        List<NewProductImage> addedImages,
        LocalDate lastRestockedDate
) {
}
