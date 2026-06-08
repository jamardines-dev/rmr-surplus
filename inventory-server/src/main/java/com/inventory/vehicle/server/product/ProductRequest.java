package com.inventory.vehicle.server.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String productName,
        @NotBlank String brand,
        @NotBlank String vehicleType,
        @NotBlank String modelCode,
        @Min(0) int currentStock,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        boolean active
) {
}
