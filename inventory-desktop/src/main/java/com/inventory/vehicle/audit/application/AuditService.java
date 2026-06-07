package com.inventory.vehicle.audit.application;

import com.inventory.vehicle.audit.domain.AuditLog;
import com.inventory.vehicle.audit.infrastructure.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(String action, String description, String performedBy) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setPerformedBy(performedBy);
        auditLogRepository.save(auditLog);
    }
}
