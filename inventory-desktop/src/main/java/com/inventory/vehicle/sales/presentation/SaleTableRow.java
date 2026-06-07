package com.inventory.vehicle.sales.presentation;

import com.inventory.vehicle.sales.domain.Sale;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SaleTableRow {

    private final Long id;
    private final String sellerName;
    private final LocalDate soldDate;
    private final BigDecimal totalAmount;
    private final String encodedBy;
    private final LocalDateTime createdAt;

    public SaleTableRow(Sale sale) {
        this.id = sale.getId();
        this.sellerName = sale.getSellerName();
        this.soldDate = sale.getSoldDate();
        this.totalAmount = sale.getTotalAmount();
        this.encodedBy = sale.getEncodedBy();
        this.createdAt = sale.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getSellerName() {
        return sellerName;
    }

    public LocalDate getSoldDate() {
        return soldDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getEncodedBy() {
        return encodedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
