package com.inventory.vehicle.sales.application;

import java.time.LocalDate;
import java.util.List;

public record RecordCartSaleCommand(
        String sellerName,
        LocalDate soldDate,
        List<CartSaleItemCommand> items
) {
}
