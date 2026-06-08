package com.inventory.vehicle.server.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(String action, String description, String performedBy) {
        this.action = action;
        this.description = description;
        this.performedBy = performedBy;
        this.createdAt = LocalDateTime.now();
    }
}
