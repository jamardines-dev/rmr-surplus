package com.inventory.vehicle.product.presentation;

import java.math.BigDecimal;

public class CartItemRow {

    private final Long productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;

    public CartItemRow(ProductTableRow product, int quantity) {
        this.productId = product.getId();
        this.productName = product.getProductName();
        this.quantity = quantity;
        this.unitPrice = product.getUnitPrice();
        this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
