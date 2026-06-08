package com.inventory.vehicle.server.sales;

import com.inventory.vehicle.server.product.Product;
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

@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantitySold;

    @Column(nullable = false)
    private BigDecimal priceSold;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    protected SaleItem() {
    }

    public SaleItem(Product product, int quantitySold, BigDecimal priceSold) {
        this.product = product;
        this.quantitySold = quantitySold;
        this.priceSold = priceSold;
        this.totalAmount = priceSold.multiply(BigDecimal.valueOf(quantitySold));
    }

    public Product getProduct() {
        return product;
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

    void attachToSale(Sale sale) {
        this.sale = sale;
    }
}
