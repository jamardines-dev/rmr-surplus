package com.inventory.vehicle.server.audit;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String action, String description, String performedBy) {
        auditLogRepository.save(new AuditLog(action, description, performedBy));
    }
}
