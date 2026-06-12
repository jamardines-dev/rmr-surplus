package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.product.application.ProductResult;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.scene.image.Image;

public class ProductTableRow {

    private final Long id;
    private final String productName;
    private final String brandName;
    private final String vehicleTypeName;
    private final String modelCode;
    private final int currentStock;
    private final BigDecimal unitPrice;
    private final byte[] productImage;
    private final String productImageType;
    private final LocalDate lastRestockedDate;
    private final boolean active;

    public ProductTableRow(ProductResult product) {
        this.id = product.id();
        this.productName = product.productName();
        this.brandName = product.brandName();
        this.vehicleTypeName = product.vehicleTypeName();
        this.modelCode = product.modelCode();
        this.currentStock = product.currentStock();
        this.unitPrice = product.unitPrice();
        this.productImage = product.productImage();
        this.productImageType = product.productImageType();
        this.lastRestockedDate = product.lastRestockedDate();
        this.active = product.active();
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

    public String getModelCode() {
        return modelCode;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public byte[] getProductImage() {
        return productImage;
    }

    public String getProductImageType() {
        return productImageType;
    }

    public LocalDate getLastRestockedDate() {
        return lastRestockedDate;
    }

    public String getLastRestockedDateText() {
        return lastRestockedDate == null ? "" : lastRestockedDate.toString();
    }

    public Image getImage() {
        if (productImage == null || productImage.length == 0) {
            return null;
        }
        return new Image(new ByteArrayInputStream(productImage));
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return productName + " (" + modelCode + ")";
    }
}
