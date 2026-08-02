package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.product.presentation.ProductTableRow;
import java.math.BigDecimal;

public class EmployeeCartItemRow {

    private final Long productId;
    private final String productName;
    private final String brandName;
    private final String vehicleTypeName;
    private final String modelCode;
    private final String stockNumber;
    private final String productLocation;
    private final BigDecimal originalPrice;
    private int quantity;
    private BigDecimal priceSold;

    public EmployeeCartItemRow(ProductTableRow product, int quantity, BigDecimal priceSold) {
        this.productId = product.getId();
        this.productName = product.getProductName();
        this.brandName = product.getBrandName();
        this.vehicleTypeName = product.getVehicleTypeName();
        this.modelCode = product.getModelCode();
        this.stockNumber = product.getLastDrNumber();
        this.productLocation = product.getProductLocation();
        this.originalPrice = priceSold;
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

    public String getStockNumber() {
        return stockNumber;
    }

    public String getProductLocation() {
        return productLocation == null ? "" : productLocation;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceSold() {
        return priceSold;
    }

    public void setPriceSold(BigDecimal priceSold) {
        this.priceSold = priceSold;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
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
