package com.dealmanager.repository;

import com.dealmanager.domain.Deal;
import com.dealmanager.domain.DealStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealRepository extends JpaRepository<Deal, Long> {

    // Fetch client and lines in one query to avoid N+1 on the list screen.
    @EntityGraph(attributePaths = {"client", "pricingLines"})
    List<Deal> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"client", "pricingLines"})
    List<Deal> findByStatusOrderByUpdatedAtDesc(DealStatus status);
}
