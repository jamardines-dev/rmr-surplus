package com.inventory.vehicle.dashboard.presentation;

import com.inventory.vehicle.sales.application.SaleLineResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RecentSaleActivityRow {

    private static final DateTimeFormatter SOLD_AT_FORMATTER = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final String sellerName;
    private final String activity;
    private final int quantitySold;
    private final BigDecimal totalAmount;
    private final LocalDateTime soldAt;
    private final String soldAtText;

    public RecentSaleActivityRow(SaleLineResult saleLine) {
        this.sellerName = saleLine.sellerName();
        this.quantitySold = saleLine.quantitySold();
        this.activity = "Sold " + saleLine.quantitySold() + " units of "
                + saleLine.brandName() + " " + saleLine.productName() + " " + saleLine.modelCode();
        this.totalAmount = saleLine.totalAmount();
        this.soldAt = saleLine.soldAt();
        this.soldAtText = saleLine.soldAt().format(SOLD_AT_FORMATTER);
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getActivity() {
        return activity;
    }

    public int getQuantitySold() {
        return quantitySold;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getSoldAt() {
        return soldAt;
    }

    public String getSoldAtText() {
        return soldAtText;
    }
}
