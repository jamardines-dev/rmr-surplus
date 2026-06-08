package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.sales.application.SaleLineResult;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class EmployeeSaleTableRow {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private final String time;
    private final Long saleItemId;
    private final String productName;
    private final String brandName;
    private final String vehicleTypeName;
    private final String modelCode;
    private final int quantitySold;
    private final BigDecimal priceSold;
    private final BigDecimal totalAmount;

    public EmployeeSaleTableRow(SaleLineResult saleLine) {
        this.time = saleLine.soldAt().format(TIME_FORMATTER);
        this.saleItemId = saleLine.saleItemId();
        this.productName = saleLine.productName();
        this.brandName = saleLine.brandName();
        this.vehicleTypeName = saleLine.vehicleTypeName();
        this.modelCode = saleLine.modelCode();
        this.quantitySold = saleLine.quantitySold();
        this.priceSold = saleLine.priceSold();
        this.totalAmount = saleLine.totalAmount();
    }

    public String getTime() {
        return time;
    }

    public Long getSaleItemId() {
        return saleItemId;
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

    public int getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getPriceSold() {
        return priceSold;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
