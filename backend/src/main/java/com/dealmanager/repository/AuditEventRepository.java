package com.dealmanager.repository;

import com.dealmanager.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByDealIdOrderByOccurredAtAscIdAsc(Long dealId);
}
