package com.dealmanager.service;

import com.dealmanager.domain.AuditAction;
import com.dealmanager.domain.AuditEvent;
import com.dealmanager.domain.Deal;
import com.dealmanager.domain.DealStatus;
import com.dealmanager.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * MANDATORY: an audit row must be written in the same transaction as the change it
     * describes, so either both commit or neither does. Calling this outside a
     * transaction is a bug and fails fast.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Deal deal, String actor, AuditAction action, DealStatus from, DealStatus to,
                       String reason, Map<String, Object> details) {
        repository.save(new AuditEvent(deal.getId(), actor, action, from, to, reason, details, Instant.now(clock)));
    }
}
