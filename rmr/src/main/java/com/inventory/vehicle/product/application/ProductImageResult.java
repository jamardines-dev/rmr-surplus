package com.inventory.vehicle.product.application;

public record ProductImageResult(
        Long id,
        byte[] imageData,
        String imageType,
        int sortOrder
) {
}
