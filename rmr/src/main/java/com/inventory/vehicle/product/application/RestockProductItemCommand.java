package com.inventory.vehicle.product.application;

public record RestockProductItemCommand(
        Long productId,
        int quantity,
        String drNumber
) {
}
