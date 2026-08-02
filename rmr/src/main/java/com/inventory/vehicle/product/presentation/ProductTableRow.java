package com.inventory.vehicle.product.presentation;

import com.inventory.vehicle.product.application.ProductImageResult;
import com.inventory.vehicle.product.application.ProductResult;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.image.Image;

public class ProductTableRow {

    private static final double THUMBNAIL_WIDTH = 180;
    private static final double THUMBNAIL_HEIGHT = 140;
    private static final double DETAIL_IMAGE_WIDTH = 900;
    private static final double DETAIL_IMAGE_HEIGHT = 700;

    private final Long id;
    private final String productName;
    private final String brandName;
    private final String vehicleTypeName;
    private final String modelCode;
    private final String productLocation;
    private final int currentStock;
    private final BigDecimal unitPrice;
    private final List<ProductImageResult> images;
    private final LocalDate lastRestockedDate;
    private final String lastDrNumber;
    private final boolean active;
    private Image thumbnailImage;
    private boolean thumbnailLoaded;
    private List<Image> detailImages;

    public ProductTableRow(ProductResult product) {
        this.id = product.id();
        this.productName = product.productName();
        this.brandName = product.brandName();
        this.vehicleTypeName = product.vehicleTypeName();
        this.modelCode = product.modelCode();
        this.productLocation = product.productLocation();
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

    public String getProductLocation() {
        return productLocation == null ? "" : productLocation;
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
        if (!thumbnailLoaded) {
            thumbnailImage = firstImageResult()
                    .map(ProductTableRow::toThumbnailImage)
                    .orElse(null);
            thumbnailLoaded = true;
        }
        return thumbnailImage;
    }

    public List<Image> getImages() {
        if (detailImages == null) {
            if (images == null || images.isEmpty()) {
                detailImages = List.of();
            } else {
                detailImages = images.stream()
                        .map(ProductImageResult::imageData)
                        .filter(ProductTableRow::hasImageData)
                        .map(ProductTableRow::toDetailImage)
                        .toList();
            }
        }
        return detailImages;
    }

    public List<ProductImageResult> getImageResults() {
        return images == null ? List.of() : images;
    }

    public boolean isActive() {
        return active;
    }

    private java.util.Optional<ProductImageResult> firstImageResult() {
        if (images == null || images.isEmpty()) {
            return java.util.Optional.empty();
        }
        return images.stream()
                .filter(image -> hasImageData(image.imageData()))
                .findFirst();
    }

    private static boolean hasImageData(byte[] imageData) {
        return imageData != null && imageData.length > 0;
    }

    private static Image toThumbnailImage(ProductImageResult image) {
        return toImage(image.imageData(), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
    }

    private static Image toDetailImage(byte[] imageData) {
        return toImage(imageData, DETAIL_IMAGE_WIDTH, DETAIL_IMAGE_HEIGHT);
    }

    private static Image toImage(byte[] imageData, double width, double height) {
        return new Image(new ByteArrayInputStream(imageData), width, height, true, true);
    }

    @Override
    public String toString() {
        return productName + " (" + modelCode + ")";
    }
}
