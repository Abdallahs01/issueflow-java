package com.att.tdp.issueflow.audit.dto;

import com.att.tdp.issueflow.audit.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String entityType,
        Long entityId,
        String action,
        String actor,
        String details,
        Instant createdAt
) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getAction(),
                auditLog.getActor(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
