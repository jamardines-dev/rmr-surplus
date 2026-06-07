package com.inventory.vehicle.sales.application;

import java.math.BigDecimal;

public record CartSaleItemCommand(
        Long productId,
        int quantitySold,
        BigDecimal priceSold
) {
}
