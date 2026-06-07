package com.inventory.vehicle.sales.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordSaleCommand(
        String sellerName,
        LocalDate soldDate,
        Long productId,
        int quantitySold,
        BigDecimal priceSold
) {
}
