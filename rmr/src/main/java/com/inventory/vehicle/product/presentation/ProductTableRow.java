package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.product.application.ProductImageResult;
import com.inventory.vehicle.product.application.ProductResult;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.image.Image;

public class ProductTableRow {

    private final Long id;
    private final String productName;
    private final String brandName;
    private final String vehicleTypeName;
    private final String modelCode;
    private final int currentStock;
    private final BigDecimal unitPrice;
    private final List<ProductImageResult> images;
    private final LocalDate lastRestockedDate;
    private final String lastDrNumber;
    private final boolean active;

    public ProductTableRow(ProductResult product) {
        this.id = product.id();
        this.productName = product.productName();
        this.brandName = product.brandName();
        this.vehicleTypeName = product.vehicleTypeName();
        this.modelCode = product.modelCode();
        this.currentStock = product.currentStock();
        this.unitPrice = product.unitPrice();
        this.images = product.images();
        this.lastRestockedDate = product.lastRestockedDate();
        this.lastDrNumber = product.lastDrNumber();
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

    public LocalDate getLastRestockedDate() {
        return lastRestockedDate;
    }

    public String getLastRestockedDateText() {
        return lastRestockedDate == null ? "" : lastRestockedDate.toString();
    }

    public String getLastDrNumber() {
        return lastDrNumber == null ? "" : lastDrNumber;
    }

    public Image getImage() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        byte[] imageData = images.get(0).imageData();
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        return new Image(new ByteArrayInputStream(imageData));
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return productName + " (" + modelCode + ")";
    }
}
