package com.inventory.vehicle.product.application;

import java.time.LocalDate;
import java.util.List;

public record RestockNewProductsCommand(
        String drNumber,
        LocalDate restockedDate,
        List<CreateProductCommand> products
) {
}
