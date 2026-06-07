package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.product.application.ProductResult;
import java.math.BigDecimal;

public class ProductTableRow {

    private final Long id;
    private final String productName;
    private final String brandName;
    private final String vehicleTypeName;
    private final int currentStock;
    private final BigDecimal unitPrice;

    public ProductTableRow(ProductResult product) {
        this.id = product.id();
        this.productName = product.productName();
        this.brandName = product.brandName();
        this.vehicleTypeName = product.vehicleTypeName();
        this.currentStock = product.currentStock();
        this.unitPrice = product.unitPrice();
    }

    public Long getId() {
        return id;
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

    public int getCurrentStock() {
        return currentStock;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @Override
    public String toString() {
        return productName + " (" + brandName + ")";
    }
}
