package com.inventory.vehicle.sales.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleLineResult(
        Long saleItemId,
        Long saleId,
        String sellerName,
        LocalDateTime soldAt,
        String productName,
        String brandName,
        String vehicleTypeName,
        String modelCode,
        String stockNumber,
        byte[] productImage,
        String productImageType,
        int quantitySold,
        BigDecimal originalPrice,
        BigDecimal priceSold,
        BigDecimal totalAmount
) {
}
