package com.inventory.vehicle.server.sales;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CartSaleItemRequest(
        @NotNull Long productId,
        @Min(1) int quantity,
        @NotNull @DecimalMin("0.00") BigDecimal priceSold
) {
}
