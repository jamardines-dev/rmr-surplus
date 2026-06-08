package com.inventory.vehicle.server.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RecordCartSaleRequest(
        @NotEmpty List<@Valid CartSaleItemRequest> items
) {
}
