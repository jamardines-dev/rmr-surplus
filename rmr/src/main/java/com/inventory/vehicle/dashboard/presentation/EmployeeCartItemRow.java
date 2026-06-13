package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.product.presentation.ProductTableRow;
import java.math.BigDecimal;

public class EmployeeCartItemRow {

    private final Long productId;
    private final String productName;
    private final String brandName;
    private final String vehicleTypeName;
    private final String modelCode;
    private int quantity;
    private final BigDecimal priceSold;

    public EmployeeCartItemRow(ProductTableRow product, int quantity, BigDecimal priceSold) {
        this.productId = product.getId();
        this.productName = product.getProductName();
        this.brandName = product.getBrandName();
        this.vehicleTypeName = product.getVehicleTypeName();
        this.modelCode = product.getModelCode();
        this.quantity = quantity;
        this.priceSold = priceSold;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getVehicleTypeName() {
        return vehicleTypeName;
    }

    public String getModelCode() {
        return modelCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceSold() {
        return priceSold;
    }

    public BigDecimal getTotalAmount() {
        return priceSold.multiply(BigDecimal.valueOf(quantity));
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }
}
