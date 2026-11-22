package com.dealmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/** Who did what, when and why. Append-only: {@code @Immutable} stops Hibernate updating rows. */
@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deal_id", nullable = false)
    private Long dealId;

    @Column(nullable = false)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private DealStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private DealStatus toStatus;

    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(Long dealId, String actor, AuditAction action, DealStatus fromStatus, DealStatus toStatus,
                      String reason, Map<String, Object> details, Instant occurredAt) {
        this.dealId = dealId;
        this.actor = actor;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.details = details;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getDealId() {
        return dealId;
    }

    public String getActor() {
        return actor;
    }

    public AuditAction getAction() {
        return action;
    }

    public DealStatus getFromStatus() {
        return fromStatus;
    }

    public DealStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
