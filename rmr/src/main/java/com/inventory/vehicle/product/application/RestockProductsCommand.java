package com.inventory.vehicle.product.application;

import java.time.LocalDate;
import java.util.List;

public record RestockProductsCommand(
        String drNumber,
        LocalDate restockedDate,
        List<RestockProductItemCommand> items
) {
}
