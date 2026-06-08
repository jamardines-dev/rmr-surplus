package com.inventory.vehicle.server.inventory;

import com.inventory.vehicle.server.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int previousStock;

    @Column(nullable = false)
    private int newStock;

    private String reason;

    private String referenceId;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected StockMovement() {
    }

    public StockMovement(Product product, StockMovementType movementType, int quantity, int previousStock, int newStock,
                         String reason, String referenceId, String createdBy) {
        this.product = product;
        this.movementType = movementType;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.reason = reason;
        this.referenceId = referenceId;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }
}
