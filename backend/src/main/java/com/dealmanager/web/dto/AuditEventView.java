package com.dealmanager.web.dto;

import com.dealmanager.domain.AuditAction;
import com.dealmanager.domain.AuditEvent;
import com.dealmanager.domain.DealStatus;

import java.time.Instant;
import java.util.Map;

public record AuditEventView(Long id, String actor, AuditAction action, DealStatus fromStatus, DealStatus toStatus,
                             String reason, Map<String, Object> details, Instant occurredAt) {

    public static AuditEventView from(AuditEvent e) {
        return new AuditEventView(e.getId(), e.getActor(), e.getAction(), e.getFromStatus(), e.getToStatus(),
                e.getReason(), e.getDetails(), e.getOccurredAt());
    }
}
