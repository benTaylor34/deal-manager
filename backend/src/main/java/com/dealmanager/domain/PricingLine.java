package com.dealmanager.domain;

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
@Table(name = "pricing_line")
public class PricingLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @Column(nullable = false)
    private String service;

    @Column(name = "standard_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal standardPrice;

    @Column(name = "proposed_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal proposedPrice;

    /** Committed annual volume (number of transactions/units per year). */
    @Column(name = "committed_volume", nullable = false)
    private Long committedVolume;

    protected PricingLine() {
    }

    public PricingLine(String service, BigDecimal standardPrice, BigDecimal proposedPrice, long committedVolume) {
        this.service = service;
        this.standardPrice = standardPrice;
        this.proposedPrice = proposedPrice;
        this.committedVolume = committedVolume;
    }

    void attachTo(Deal deal) {
        this.deal = deal;
    }

    public Long getId() {
        return id;
    }

    public Deal getDeal() {
        return deal;
    }

    public String getService() {
        return service;
    }

    public BigDecimal getStandardPrice() {
        return standardPrice;
    }

    public BigDecimal getProposedPrice() {
        return proposedPrice;
    }

    public long getCommittedVolume() {
        return committedVolume;
    }
}
