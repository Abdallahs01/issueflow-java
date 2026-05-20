package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.audit.dto.AuditLogResponse;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String entityType, Long entityId, String action, String actor, String details) {
        auditLogRepository.save(new AuditLog(entityType, entityId, action, actor, details));
    }

    @Transactional
    public void recordCurrentUserAction(String entityType, Long entityId, String action, String details) {
        auditLogRepository.save(new AuditLog(entityType, entityId, action, currentActor(), details));
    }

    @Transactional
    public void recordSystemAction(String entityType, Long entityId, String action, String details) {
        auditLogRepository.save(new AuditLog(entityType, entityId, action, "SYSTEM", details));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAuditLogs(String entityType, Long entityId, String action, String actor) {
        Specification<AuditLog> specification = Specification.where(hasValue("entityType", entityType))
                .and(hasValue("entityId", entityId))
                .and(hasValue("action", action))
                .and(hasValue("actor", actor));

        return auditLogRepository.findAll(specification)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private <T> Specification<AuditLog> hasValue(String fieldName, T value) {
        return (root, query, builder) -> value == null ? builder.conjunction() : builder.equal(root.get(fieldName), value);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "SYSTEM";
        }

        return authentication.getName();
    }
}
