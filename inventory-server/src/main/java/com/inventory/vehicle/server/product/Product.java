package com.inventory.vehicle.server.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @Column(nullable = false, unique = true)
    private String modelCode;

    @Column(nullable = false)
    private int currentStock;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {
    }

    public Product(String productName, Brand brand, VehicleType vehicleType, String modelCode, int currentStock, BigDecimal unitPrice) {
        this.productName = productName;
        this.brand = brand;
        this.vehicleType = vehicleType;
        this.modelCode = modelCode;
        this.currentStock = currentStock;
        this.unitPrice = unitPrice;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public Brand getBrand() {
        return brand;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
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

    public boolean isActive() {
        return active;
    }

    public void updateDetails(String productName, Brand brand, VehicleType vehicleType, String modelCode, BigDecimal unitPrice, boolean active) {
        this.productName = productName;
        this.brand = brand;
        this.vehicleType = vehicleType;
        this.modelCode = modelCode;
        this.unitPrice = unitPrice;
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseStock(int quantity) {
        this.currentStock += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseStock(int quantity) {
        if (quantity > currentStock) {
            throw new IllegalArgumentException("Insufficient stock.");
        }
        this.currentStock -= quantity;
        this.updatedAt = LocalDateTime.now();
    }
}
