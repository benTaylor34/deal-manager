package com.dealmanager.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "deal")
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DealStatus status = DealStatus.DRAFT;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Set on submit; records which authority the deal was routed to. */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_authority")
    private ApprovalAuthority requiredAuthority;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic lock. Hibernate adds {@code WHERE version = ?} to every update and
     * throws if another transaction got there first. The service also compares the
     * client's If-Match version, which catches edits made from a stale screen.
     */
    @Version
    private Long version;

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PricingLine> pricingLines = new ArrayList<>();

    protected Deal() {
    }

    public Deal(Client client, String name, LocalDate startDate, LocalDate endDate, String createdBy, Instant now) {
        this.client = client;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addPricingLine(PricingLine line, Instant now) {
        if (status != DealStatus.DRAFT) {
            throw new DealRuleViolationException("Pricing lines can only be changed while the deal is DRAFT");
        }
        line.attachTo(this);
        pricingLines.add(line);
        // The collection is the inverse side, so adding a line would not bump @Version on
        // its own. Touching a column on the deal makes the deal dirty, so the version moves.
        touch(now);
    }

    public void moveTo(DealStatus newStatus, Instant now) {
        this.status = newStatus;
        touch(now);
    }

    public void routeTo(ApprovalAuthority authority) {
        this.requiredAuthority = authority;
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public String getName() {
        return name;
    }

    public DealStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ApprovalAuthority getRequiredAuthority() {
        return requiredAuthority;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<PricingLine> getPricingLines() {
        return Collections.unmodifiableList(pricingLines);
    }
}
