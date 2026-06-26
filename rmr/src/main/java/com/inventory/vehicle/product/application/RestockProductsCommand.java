package com.inventory.vehicle.product.application;

import java.time.LocalDate;
import java.util.List;

public record RestockProductsCommand(
        LocalDate restockedDate,
        List<RestockProductItemCommand> items
) {
}
